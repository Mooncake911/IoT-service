package com.iot.alerts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.alerts.engine.RuleEngine;
import com.iot.contracts.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
    private final RuleEngine ruleEngine;
    private final AlertPersistence alertPersistence;
    private final ObjectMapper objectMapper;
    private final RabbitAdmin rabbitAdmin;

    @Value("${app.rabbitmq.alerts.queue.name}")
    private String queueName;

    @Value("${app.rabbitmq.alerts.batching.size}")
    private int batchSize;

    @Value("${app.rabbitmq.alerts.batching.timeout-ms}")
    private int timeoutMs;

    @Value("${app.rabbitmq.alerts.batching.concurrency}")
    private int concurrency;

    public AmqpConsumer(Receiver receiver, 
                        RuleEngine ruleEngine, 
                        AlertPersistence alertPersistence,
                        ObjectMapper objectMapper,
                        RabbitAdmin rabbitAdmin) {
        this.receiver = receiver;
        this.ruleEngine = ruleEngine;
        this.alertPersistence = alertPersistence;
        this.objectMapper = objectMapper;
        this.rabbitAdmin = rabbitAdmin;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        rabbitAdmin.initialize();
        log.info("Starting Reactive RabbitMQ Consumer for queue: {}", queueName);
        
        // QoS (prefetch count) is the key to reactive backpressure in RabbitMQ
        ConsumeOptions options = new ConsumeOptions().qos(batchSize * 2);
        
        receiver.consumeManualAck(queueName, options)
                .flatMap(delivery -> {
                    List<DeviceData> devices = deserialize(delivery.getBody());
                    
                    return Flux.fromIterable(devices)
                            // Process rules for each device in parallel
                            .flatMap(deviceData -> Mono.fromCallable(() -> ruleEngine.processDevice(deviceData))
                                    .subscribeOn(Schedulers.boundedElastic()), concurrency)
                            .flatMap(Flux::fromIterable)
                            // Batch alerts for DB persistence
                            .bufferTimeout(batchSize, Duration.ofMillis(timeoutMs))
                            .flatMap(alerts -> {
                                if (!alerts.isEmpty()) {
                                    return alertPersistence.saveBatch(alerts);
                                }
                                return Mono.empty();
                            })
                            .then(Mono.fromRunnable(delivery::ack))
                            .onErrorResume(e -> {
                                log.error("Error processing delivery: {}", e.getMessage());
                                delivery.nack(false); // Reject without requeue for corrupted data
                                return Mono.empty();
                            });
                }, concurrency)
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30)))
                .subscribe(
                    v -> {},
                    e -> log.error("FATAL: Reactive Alerts pipeline terminated: {}", e.getMessage())
                );
    }

    private List<DeviceData> deserialize(byte[] body) {
        try {
            if (body == null || body.length == 0) return List.of();
            // Handle both single objects and lists
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
