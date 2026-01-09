package com.iot.controller.service;

import com.iot.controller.config.RabbitConfig;
import com.iot.controller.domain.AlertEntity;
import com.iot.controller.repository.AlertRepository;
import com.iot.shared.domain.AlertTriggered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Consumes AlertTriggered events from Rule Engine and persists them to MongoDB.
 */
@Service
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final AlertRepository alertRepository;

    public AlertConsumer(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Receives AlertTriggered messages from alerts.queue.
     * Messages are published by Rule Engine to alerts.exchange (FanoutExchange).
     *
     * @param alert The alert from Rule Engine
     */
    @RabbitListener(queues = RabbitConfig.ALERTS_QUEUE_NAME)
    public void consumeAlert(AlertTriggered alert) {
        log.info("Received alert: {} for device {}", alert.ruleId(), alert.deviceId());

        try {
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

            alertRepository.save(entity)
                    .doOnSuccess(saved -> log.debug("Alert saved: {}", saved.getAlertId()))
                    .doOnError(error -> log.error("Failed to save alert: {}", alert.alertId(), error))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error processing alert {}: {}", alert.alertId(), e.getMessage(), e);
        }
    }
}
