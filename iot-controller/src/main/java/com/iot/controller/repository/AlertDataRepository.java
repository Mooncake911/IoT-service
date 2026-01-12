package com.iot.controller.repository;

import com.iot.controller.domain.AlertEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertDataRepository extends ReactiveMongoRepository<AlertEntity, String> {
}
