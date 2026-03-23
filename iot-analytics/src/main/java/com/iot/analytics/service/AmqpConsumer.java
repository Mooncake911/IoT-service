package com.iot.analytics.service;

import com.iot.shared.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Service
@Slf4j
public class AmqpConsumer {

    private final Sinks.Many<DeviceData> sink;

    public AmqpConsumer(AnalyticsService analyticsService,
                        AnalyticsPublisher analyticsPublisher,
                        @Value("${app.rabbitmq.analytics.batching.size}") int outputBatchSize,
                        @Value("${app.rabbitmq.analytics.batching.timeout-ms}") int timeoutMs,
                        @Value("${app.rabbitmq.analytics.batching.buffer-limit}") int bufferLimit,
                        @Value("${app.rabbitmq.analytics.batching.concurrency}") int concurrency) {

        this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferLimit, false);

        analyticsService.getBatchSizeFlux()
                .distinctUntilChanged()
                .switchMap(calculationBatchSize -> {
                    log.info("Configuring analytics pipeline: calcWindow={}, transportBatch={}, concurrency={}",
                            calculationBatchSize, outputBatchSize, concurrency);

                    return this.sink.asFlux()
                            // 1. Группировка для расчетов (бизнес-логика)
                            .bufferTimeout(calculationBatchSize, Duration.ofMillis(timeoutMs))
                            .flatMap(batch -> {
                                if (batch.isEmpty()) return reactor.core.publisher.Mono.empty();

                                return analyticsService.calculateStats(batch)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        // Не даем одной ошибке расчета убить весь поток
                                        .onErrorResume(e -> {
                                            log.error("Analytics calculation failed for batch: {}", e.getMessage());
                                            return reactor.core.publisher.Mono.empty();
                                        });
                            }, concurrency)
                            // 2. Группировка для отправки результатов (транспортная логика)
                            .bufferTimeout(outputBatchSize, Duration.ofMillis(timeoutMs))
                            .doOnNext(analyticsBatch -> {
                                if (!analyticsBatch.isEmpty()) {
                                    analyticsPublisher.publishBatch(analyticsBatch);
                                }
                            });
                })
                // Перезапуск всей цепочки при фатальных сбоях
                .retry()
                .subscribe(
                        v -> {},
                        e -> log.error("FATAL: Analytics pipeline terminated", e)
                );
    }

    @RabbitListener(queues = "${app.rabbitmq.analytics.queue.name}")
    public void consumeMessage(List<DeviceData> deviceDataList) {
        log.debug("Received {} devices from RabbitMQ", deviceDataList.size());

        for (DeviceData deviceData : deviceDataList) {
            // Реализация ручного Backpressure:
            // Мы пытаемся "запихнуть" данные во внутренний буфер (sink).
            // Если буфер полон (FAIL_OVERFLOW), мы блокируем поток RabbitMQ.
            while (true) {
                Sinks.EmitResult result = sink.tryEmitNext(deviceData);

                if (result.isSuccess()) {
                    break;
                }

                if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
                    // Буфер переполнен. Спим 10мс и пробуем снова.
                    // Это заставляет данные копиться в RabbitMQ, а не в RAM.
                    LockSupport.parkNanos(10_000_000);
                    continue;
                }

                if (result.isFailure()) {
                    log.error("Critical error emitting to sink: {}", result);
                    break;
                }
            }
        }
    }
}