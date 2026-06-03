package com.iot.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.contracts.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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
    private final RabbitAdmin rabbitAdmin;

    private final String queueName;
    private final int concurrency;

    public AmqpConsumer(Receiver receiver,
                        AnalyticsService analyticsService,
                        AnalyticsPersistence analyticsPersistence,
                        LiveAnalyticsService liveAnalyticsService,
                        ObjectMapper objectMapper,
                        RabbitAdmin rabbitAdmin,
                        @org.springframework.beans.factory.annotation.Value("${app.rabbitmq.analytics.queue.name}") String queueName,
                        @org.springframework.beans.factory.annotation.Value("${app.rabbitmq.analytics.concurrency}") int concurrency) {
        this.receiver = receiver;
        this.analyticsService = analyticsService;
        this.analyticsPersistence = analyticsPersistence;
        this.liveAnalyticsService = liveAnalyticsService;
        this.objectMapper = objectMapper;
        this.rabbitAdmin = rabbitAdmin;
        this.queueName = queueName;
        this.concurrency = concurrency;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        rabbitAdmin.initialize();
        log.info("Starting Reactive RabbitMQ Consumer for Analytics queue: {}", queueName);

        analyticsService.getWindowDurationFlux()
                .distinctUntilChanged()
                .switchMap(windowSeconds -> {
                    log.info("Configuring reactive analytics pipeline: windowSeconds={}, concurrency={}",
                            windowSeconds, concurrency);

                    int prefetch = Math.max(32, concurrency * 16);

                    Flux<DeviceData> deviceStream = receiver.consumeManualAck(queueName, new reactor.rabbitmq.ConsumeOptions().qos(prefetch))
                            .flatMap(delivery -> {
                                List<DeviceData> devices = deserialize(delivery.getBody());
                                liveAnalyticsService.ingestDevices(devices);
                                return Mono.fromRunnable(delivery::ack)
                                        .thenMany(Flux.fromIterable(devices))
                                        .onErrorResume(e -> {
                                            log.error("Error processing delivery: {}", e.getMessage());
                                            delivery.nack(false);
                                            return Mono.empty();
                                        });
                            }, concurrency);

                    return deviceStream
                            .bufferTimeout(Integer.MAX_VALUE, Duration.ofSeconds(windowSeconds))
                            .flatMap(batch -> {
                                if (batch.isEmpty()) {
                                    return Mono.empty();
                                }
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
