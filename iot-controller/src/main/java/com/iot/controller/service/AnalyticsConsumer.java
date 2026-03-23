package com.iot.controller.service;

import com.iot.controller.domain.AnalyticsEntity;
import com.iot.controller.repository.AnalyticsDataRepository;
import com.iot.shared.domain.AnalyticsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Service
@Slf4j
public class AnalyticsConsumer {

    private final AnalyticsDataRepository repository;
    private final Sinks.Many<AnalyticsEntity> sink;

    public AnalyticsConsumer(AnalyticsDataRepository repository,
                             @Value("${app.rabbitmq.analytics.batching.size}") int batchSize,
                             @Value("${app.rabbitmq.analytics.batching.timeout-ms}") int timeoutMs,
                             @Value("${app.rabbitmq.analytics.batching.buffer-limit}") int bufferLimit,
                             @Value("${app.rabbitmq.analytics.batching.concurrency}") int concurrency) {

        this.repository = repository;

        // 1. Используем multicast для поддержки переподписки при retry
        // false отключает хранение истории для новых подписчиков (нам нужны только живые данные)
        this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferLimit, false);

        // Реактивный конвейер для пакетного сохранения в БД
        this.sink.asFlux()
                .bufferTimeout(batchSize, Duration.ofMillis(timeoutMs))
                .flatMap(batch -> {
                    if (batch.isEmpty()) return Mono.empty();

                    log.info("Batch saving {} analytics records to database", batch.size());
                    return this.repository.saveAll(batch)
                            .then()
                            .onErrorResume(e -> {
                                log.error("Failed to save analytics batch to DB: {}", e.getMessage());
                                return Mono.empty(); // Игнорируем ошибку батча, чтобы не прерывать поток
                            });
                }, concurrency)
                .doOnError(error -> log.error("Pipeline error in analytics saving: {}", error.getMessage()))
                // 2. Авто-реанимация конвейера при критических сбоях
                .retry()
                .subscribe(
                        v -> {},
                        e -> log.error("FATAL: Analytics save pipeline terminated!", e)
                );
    }

    @RabbitListener(queues = "${app.rabbitmq.analytics.queue.name}")
    public void consumeAnalytics(List<AnalyticsData> dataList) {
        log.debug("Received {} analytics records from RabbitMQ", dataList.size());

        for (AnalyticsData data : dataList) {
            AnalyticsEntity entity = new AnalyticsEntity(
                    null,
                    data.deviceId(),
                    data.timestamp(),
                    data.metrics());

            // 3. РУЧНОЙ БЛОКИРУЮЩИЙ ЦИКЛ (Backpressure Bridge)
            // Если база данных тормозит и внутренний буфер (bufferLimit) заполнен,
            // этот поток будет ждать здесь, не давая RabbitMQ присылать новые данные.
            while (true) {
                Sinks.EmitResult result = sink.tryEmitNext(entity);

                if (result.isSuccess()) {
                    break; // Данные успешно добавлены в очередь на сохранение
                }

                if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
                    // Память сервиса заполнена. Спим 10мс, чтобы дать БД время разгрести очередь.
                    // Данные в это время надежно копятся в очереди RabbitMQ.
                    LockSupport.parkNanos(10_000_000);
                    continue;
                }

                // В случае фатальной поломки Sink (например, при выключении приложения)
                if (result.isFailure()) {
                    log.error("Critical failure emitting to Analytics Sink: {}", result);
                    break;
                }
            }
        }
    }
}