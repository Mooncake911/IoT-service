package com.iot.analytics.service;

import com.iot.analytics.domain.AnalyticsEntity;
import com.iot.analytics.repository.AnalyticsDataRepository;
import com.iot.contracts.domain.AnalyticsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AnalyticsPersistence {

    private final AnalyticsDataRepository repository;

    public AnalyticsPersistence(AnalyticsDataRepository repository) {
        this.repository = repository;
    }

    public Mono<Void> save(AnalyticsData data) {
        AnalyticsEntity entity = toEntity(data);
        return repository.save(entity)
                .doOnSuccess(saved -> log.debug("Saved analytics for device {}", data.deviceId()))
                .doOnError(
                        e -> log.error("Failed to save analytics for device {}: {}", data.deviceId(), e.getMessage()))
                .then();
    }

    private AnalyticsEntity toEntity(AnalyticsData data) {
        return new AnalyticsEntity(
                null,
                data.deviceId(),
                data.timestamp(),
                data.metrics());
    }
}
