package com.iot.controller.service;

import com.iot.controller.domain.AnalyticsEntity;
import com.iot.controller.repository.AnalyticsDataRepository;
import com.iot.shared.domain.AnalyticsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsDataRepository repository;

    @RabbitListener(queues = "${app.rabbitmq.queue.persistence}")
    public void consumeAnalyticsData(AnalyticsData data) {
        log.debug("Received analytics data for device {}", data.deviceId());

        AnalyticsEntity entity = new AnalyticsEntity(
                null,
                data.deviceId(),
                data.timestamp(),
                data.metrics());

        repository.save(entity)
                .doOnSuccess(_ -> log.trace("Analytics data saved for device {}", data.deviceId()))
                .doOnError(error -> log.error("Failed to save analytics data for device {}: {}", data.deviceId(),
                        error.getMessage()))
                .subscribe();
    }
}
