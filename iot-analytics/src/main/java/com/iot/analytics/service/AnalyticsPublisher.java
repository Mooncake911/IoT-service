package com.iot.analytics.service;

import com.iot.shared.domain.AnalyticsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnalyticsPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.analytics}")
    private String analyticsExchangeName;

    public AnalyticsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(AnalyticsData data) {
        try {
            log.trace("Publishing stats for device {}", data.deviceId());
            rabbitTemplate.convertAndSend(analyticsExchangeName, "", data);
        } catch (Exception e) {
            log.error("Failed to publish analytics data for device {}: {}", data.deviceId(), e.getMessage());
        }
    }
}
