package com.iot.ruleengine.service;

import com.iot.shared.domain.AlertData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AlertPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.alerts}")
    private String alertsExchangeName;

    @Value("${app.rabbitmq.chunk-size}")
    private int publishChunkSize;

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

    public void publishBatch(List<AlertData> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        for (int i = 0; i < alerts.size(); i += publishChunkSize) {
            List<AlertData> chunk = alerts.subList(i, Math.min(i + publishChunkSize, alerts.size()));
            try {
                log.info("Publishing chunk of {} alerts to {}", chunk.size(), alertsExchangeName);
                rabbitTemplate.convertAndSend(alertsExchangeName, "", chunk);
            } catch (Exception e) {
                log.error("Failed to publish alert chunk [{}-{}]: {}",
                        i, Math.min(i + publishChunkSize, alerts.size()), e.getMessage());
            }
        }
    }
}
