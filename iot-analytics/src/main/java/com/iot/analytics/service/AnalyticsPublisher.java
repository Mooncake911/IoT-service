package com.iot.analytics.service;

import com.iot.shared.domain.AnalyticsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AnalyticsPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.analytics}")
    private String analyticsExchangeName;

    @Value("${app.rabbitmq.chunk-size}")
    private int publishChunkSize;

    public AnalyticsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(AnalyticsData data) {
        try {
            rabbitTemplate.convertAndSend(analyticsExchangeName, "", List.of(data));
        } catch (Exception e) {
            log.error("Failed to publish analytics data: {}", e.getMessage());
        }
    }

    public void publishBatch(List<AnalyticsData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        for (int i = 0; i < dataList.size(); i += publishChunkSize) {
            List<AnalyticsData> chunk = dataList.subList(i, Math.min(i + publishChunkSize, dataList.size()));
            try {
                log.trace("Publishing chunk of {} analytics records", chunk.size());
                rabbitTemplate.convertAndSend(analyticsExchangeName, "", chunk);
            } catch (Exception e) {
                log.error("Failed to publish analytics chunk: {}", e.getMessage());
            }
        }
    }
}