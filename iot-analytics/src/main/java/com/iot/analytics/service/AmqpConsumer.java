package com.iot.analytics.service;

import com.iot.shared.domain.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmqpConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = "${app.rabbitmq.queue.analytics}")
    public void consumeMessage(List<DeviceData> deviceData) {
        log.debug("Received batch of {} devices via AMQP", deviceData.size());
        analyticsService.calculateAndPublishStats(deviceData).subscribe();
    }
}
