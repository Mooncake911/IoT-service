package com.iot.alerts.repository;

import com.iot.alerts.domain.RuleEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RuleRepository extends ReactiveMongoRepository<RuleEntity, String> {

    Flux<RuleEntity> findAllByEnabledTrue();
}
