package com.iot.ruleengine.service;

import com.iot.ruleengine.engine.RuleEngine;
import com.iot.shared.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

@Service
@Slf4j
public class AmqpConsumer {

    private final Sinks.Many<DeviceData> sink;

    public AmqpConsumer(RuleEngine ruleEngine,
                        AlertPublisher alertPublisher,
                        @Value("${app.rabbitmq.rule-engine.batching.size}") int batchSize,
                        @Value("${app.rabbitmq.rule-engine.batching.timeout-ms}") int timeoutMs,
                        @Value("${app.rabbitmq.rule-engine.batching.buffer-limit}") int bufferLimit,
                        @Value("${app.rabbitmq.rule-engine.batching.concurrency}") int concurrency) {

        this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferLimit, false);

        this.sink.asFlux()
                // Параллельная обработка правил для устройств
                .flatMap(deviceData -> Mono.fromCallable(() -> ruleEngine.processDevice(deviceData))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("Error processing rules for device {}: {}", deviceData.id(), e.getMessage());
                            return Mono.empty();
                        }), concurrency)
                .flatMap(Flux::fromIterable)
                // Группируем алерты в пачки для эффективной отправки в RabbitMQ
                .bufferTimeout(batchSize, Duration.ofMillis(timeoutMs))
                .doOnNext(alerts -> {
                    if (!alerts.isEmpty()) {
                        log.info("Publishing batch of {} alerts to IoT-Controller", alerts.size());
                        alertPublisher.publishBatch(alerts);
                    }
                })
                .doOnError(error -> log.error("Pipeline error in rule processing: {}", error.getMessage()))
                // Перезапуск всей цепочки при фатальных сбоях
                .retry()
                .subscribe(
                        v -> {},
                        e -> log.error("FATAL: Rule Engine pipeline terminated: {}", e.getMessage())
                );
    }

    @RabbitListener(queues = "${app.rabbitmq.rule-engine.queue.name}")
    public void consumeDevice(List<DeviceData> deviceDataList) {
        log.debug("Received batch of {} devices for rule processing", deviceDataList.size());

        for (DeviceData deviceData : deviceDataList) {
            // Реализация ручного Backpressure:
            // Если внутренний буфер (bufferLimit) полон, поток RabbitMQ будет ждать здесь.
            // Это заставляет данные копиться в очереди RabbitMQ, а не в RAM сервиса.
            while (true) {
                Sinks.EmitResult result = sink.tryEmitNext(deviceData);

                if (result.isSuccess()) {
                    break;
                }

                if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
                    // Буфер полон. Спим 10мс и пробуем снова.
                    // Это заставляет данные копиться в RabbitMQ, а не в RAM.
                    LockSupport.parkNanos(10_000_000);
                    continue;
                }

                if (result.isFailure()) {
                    log.error("Sink failure in Rule Engine: {}. Message dropped.", result);
                    break;
                }
            }
        }
    }
}