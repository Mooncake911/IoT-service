package com.iot.ruleengine.service;

import com.iot.shared.domain.AlertData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.alerts}")
    private String alertsExchangeName;

    public AlertPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(AlertData alert) {
        try {
            log.debug("Publishing alert: {} for device {}", alert.ruleId(), alert.deviceId());
            rabbitTemplate.convertAndSend(alertsExchangeName, "", alert);
        } catch (Exception e) {
            log.error("Failed to publish alert for device {}: {}", alert.deviceId(), e.getMessage());
        }
    }
}
