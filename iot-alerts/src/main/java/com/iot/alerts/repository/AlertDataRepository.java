package com.iot.alerts.repository;

import com.iot.alerts.domain.AlertEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AlertDataRepository extends ReactiveMongoRepository<AlertEntity, String> {

    Flux<AlertEntity> findAllByOrderByReceivedAtDesc(Pageable pageable);
}

