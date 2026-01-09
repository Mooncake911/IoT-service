package com.iot.ruleengine.service;

import com.iot.ruleengine.config.RabbitConfig;
import com.iot.shared.domain.AlertTriggered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes AlertTriggered events to RabbitMQ alerts.exchange.
 * IoT Controller subscribes to this exchange to persist alerts to MongoDB.
 */
@Service
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public AlertPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes an alert to the alerts exchange.
     *
     * @param alert The alert to publish
     */
    public void publish(AlertTriggered alert) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.ALERTS_EXCHANGE_NAME, "", alert);
            log.debug("Published alert: {} for device {}", alert.ruleId(), alert.deviceId());
        } catch (Exception e) {
            log.error("Failed to publish alert: {}", alert, e);
        }
    }
}
