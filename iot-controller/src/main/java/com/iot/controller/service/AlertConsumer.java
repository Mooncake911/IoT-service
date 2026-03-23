package com.iot.controller.service;

import com.iot.controller.domain.AlertEntity;
import com.iot.controller.repository.AlertDataRepository;
import com.iot.shared.domain.AlertData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Service
@Slf4j
public class AlertConsumer {

    private final AlertDataRepository alertDataRepository;
    private final Sinks.Many<AlertEntity> sink;

    public AlertConsumer(AlertDataRepository alertDataRepository,
                         @Value("${app.rabbitmq.alerts.batching.size}") int batchSize,
                         @Value("${app.rabbitmq.alerts.batching.timeout-ms}") int timeoutMs,
                         @Value("${app.rabbitmq.alerts.batching.buffer-limit}") int bufferLimit,
                         @Value("${app.rabbitmq.alerts.batching.concurrency}") int concurrency) {

        this.alertDataRepository = alertDataRepository;

        // 1. Multicast позволяет переподписываться (нужно для retry)
        // false означает, что нам не нужно хранить историю для новых подписчиков
        this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferLimit, false);

        // Поток сохранения в БД
        this.sink.asFlux()
                .bufferTimeout(batchSize, Duration.ofMillis(timeoutMs))
                .flatMap(batch -> {
                    if (batch.isEmpty()) {
                        return Mono.empty();
                    }
                    log.info("Batch saving {} alert records to database", batch.size());
                    return this.alertDataRepository.saveAll(batch)
                            .then()
                            .onErrorResume(e -> {
                                log.error("Failed to save alert batch to DB: {}", e.getMessage());
                                return Mono.empty(); // Продолжаем работу даже после ошибки в одном батче
                            });
                }, concurrency)
                .doOnError(error -> log.error("Pipeline error in alert saving: {}", error.getMessage()))
                // 2. Авто-реанимация: если произойдет критический сбой, поток перезапустится
                .retry()
                .subscribe(
                        v -> {},
                        e -> log.error("FATAL: Alert consumer pipeline terminated!", e)
                );
    }

    @RabbitListener(queues = "${app.rabbitmq.alerts.queue.name}")
    public void consumeAlert(List<AlertData> alerts) {
        log.debug("Received {} alerts from RabbitMQ", alerts.size());

        for (AlertData alert : alerts) {
            AlertEntity entity = new AlertEntity(
                    alert.alertId(),
                    alert.deviceId(),
                    alert.ruleId(),
                    alert.ruleName(),
                    alert.severity(),
                    alert.currentValue(),
                    alert.threshold(),
                    alert.timestamp(),
                    alert.ruleType(),
                    Instant.now());

            // 3. РУЧНОЙ ЦИКЛ ОБРАТНОГО ДАВЛЕНИЯ (Backpressure)
            // Мы не используем стандартный emitNext, чтобы избежать OverflowException, которое убивает Flux.
            while (true) {
                Sinks.EmitResult result = sink.tryEmitNext(entity);

                if (result.isSuccess()) {
                    break; // Данные приняты в буфер
                }

                if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
                    // Буфер (bufferLimit) переполнен. Значит база данных не успевает.
                    // Приостанавливаем поток RabbitMQ на 10мс и пробуем снова.
                    // Это заставляет данные копиться в RabbitMQ, а не в RAM.
                    LockSupport.parkNanos(10_000_000); // 10ms
                    continue;
                }

                // Если Sink закрыт или произошла иная фатальная ошибка
                if (result.isFailure()) {
                    log.error("Critical failure emitting to Alert Sink: {}", result);
                    break;
                }
            }
        }
    }
}