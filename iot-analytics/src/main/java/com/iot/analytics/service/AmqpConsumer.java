package com.iot.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.contracts.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class AmqpConsumer {

    private final Receiver receiver;
    private final AnalyticsService analyticsService;
    private final AnalyticsPersistence analyticsPersistence;
    private final LiveAnalyticsService liveAnalyticsService;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.analytics.queue.name}")
    private String queueName;

    @Value("${app.rabbitmq.analytics.batching.timeout-ms}")
    private int timeoutMs;

    @Value("${app.rabbitmq.analytics.batching.concurrency}")
    private int concurrency;

    public AmqpConsumer(Receiver receiver,
                        AnalyticsService analyticsService,
                        AnalyticsPersistence analyticsPersistence,
                        LiveAnalyticsService liveAnalyticsService,
                        ObjectMapper objectMapper) {
        this.receiver = receiver;
        this.analyticsService = analyticsService;
        this.analyticsPersistence = analyticsPersistence;
        this.liveAnalyticsService = liveAnalyticsService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Starting Reactive RabbitMQ Consumer for Analytics queue: {}", queueName);

        // This pipeline dynamically recreates the RabbitMQ consumer if the calculation batch size changes
        // This allows us to adjust the prefetch (QoS) on the fly for optimal throughput.
        analyticsService.getBatchSizeFlux()
                .distinctUntilChanged()
                .switchMap(calculationBatchSize -> {
                    log.info("Configuring reactive analytics pipeline: calcWindow={}, concurrency={}",
                            calculationBatchSize, concurrency);

                    // We set prefetch to 2x the calculation window to keep the pipeline busy
                    ConsumeOptions options = new ConsumeOptions().qos(calculationBatchSize * 2);

                    Flux<DeviceData> deviceStream = receiver.consumeManualAck(queueName, options)
                            .flatMap(delivery -> {
                                List<DeviceData> devices = deserialize(delivery.getBody());
                                return Mono.fromRunnable(delivery::ack)
                                        .thenMany(Flux.fromIterable(devices))
                                        .onErrorResume(e -> {
                                            log.error("Error processing delivery: {}", e.getMessage());
                                            delivery.nack(false);
                                            return Mono.empty();
                                        });
                            }, concurrency);

                    return deviceStream
                            .bufferTimeout(calculationBatchSize, Duration.ofMillis(timeoutMs))
                            .flatMap(batch -> {
                                if (batch.isEmpty()) {
                                    return Mono.empty();
                                }
                                liveAnalyticsService.ingestBatch(batch);
                                return analyticsService.calculateStats(batch)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(analyticsPersistence::save)
                                        .onErrorResume(e -> {
                                            log.error("Analytics calculation failed: {}", e.getMessage());
                                            return Mono.empty();
                                        });
                            }, concurrency);
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30)))
                .subscribe(
                    v -> {},
                    e -> log.error("FATAL: Reactive Analytics pipeline terminated: {}", e.getMessage())
                );
    }

    private List<DeviceData> deserialize(byte[] body) {
        try {
            if (body == null || body.length == 0) return List.of();
            if (body[0] == '[') {
                return objectMapper.readValue(body, new TypeReference<List<DeviceData>>() {});
            } else {
                return List.of(objectMapper.readValue(body, DeviceData.class));
            }
        } catch (Exception e) {
            log.error("Failed to deserialize message: {}", e.getMessage());
            return List.of();
        }
    }
}
