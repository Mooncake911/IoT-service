package com.iot.alerts.service;

import com.iot.alerts.domain.AlertEntity;
import com.iot.alerts.repository.AlertDataRepository;
import com.iot.contracts.domain.AlertData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class AlertPersistence {

    private final AlertDataRepository repository;

    public AlertPersistence(AlertDataRepository repository) {
        this.repository = repository;
    }

    public Mono<Void> save(AlertData data) {
        AlertEntity entity = toEntity(data);
        return repository.save(entity)
                .doOnSuccess(saved -> log.debug("Saved alert for device {}", data.deviceId()))
                .doOnError(e -> log.error("Failed to save alert for device {}: {}", data.deviceId(), e.getMessage()))
                .then();
    }

    public Mono<Void> saveBatch(List<AlertData> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return Mono.empty();
        }

        List<AlertEntity> entities = alerts.stream().map(this::toEntity).toList();
        return repository.saveAll(entities)
                .doOnComplete(() -> log.debug("Saved batch of {} alerts", entities.size()))
                .then();
    }

    private AlertEntity toEntity(AlertData data) {
        return new AlertEntity(
                null,
                data.deviceId(),
                data.ruleId(),
                data.ruleName(),
                data.severity(),
                data.currentValue(),
                data.threshold(),
                data.timestamp(),
                data.ruleType(),
                Instant.now());
    }
}

