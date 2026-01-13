package com.iot.controller.service;

import com.iot.controller.domain.AlertEntity;
import com.iot.controller.repository.AlertDataRepository;
import com.iot.shared.domain.AlertData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertConsumer {

    private final AlertDataRepository alertDataRepository;

    @RabbitListener(queues = "${app.rabbitmq.queue.alerts.name}")
    public void consumeAlert(AlertData alert) {
        log.info("Received alert: {} for device {}", alert.ruleId(), alert.deviceId());

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

        alertDataRepository.save(entity)
                .doOnSuccess(_ -> log.debug("Alert saved: {}", alert.alertId()))
                .doOnError(error -> log.error("Failed to save alert {}: {}", alert.alertId(), error.getMessage()))
                .subscribe();
    }
}
