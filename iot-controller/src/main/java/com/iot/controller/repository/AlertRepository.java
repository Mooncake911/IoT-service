package com.iot.controller.repository;

import com.iot.controller.domain.AlertEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Reactive MongoDB repository for Alert entities.
 */
@Repository
public interface AlertRepository extends ReactiveMongoRepository<AlertEntity, String> {

    /**
     * Find alerts by device ID, ordered by received time descending.
     */
    Flux<AlertEntity> findByDeviceIdOrderByReceivedAtDesc(long deviceId);

    /**
     * Find alerts by severity level.
     */
    Flux<AlertEntity> findBySeverityOrderByReceivedAtDesc(String severity);

    /**
     * Find alerts by rule ID.
     */
    Flux<AlertEntity> findByRuleIdOrderByReceivedAtDesc(String ruleId);
}
