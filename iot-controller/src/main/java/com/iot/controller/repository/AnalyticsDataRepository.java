package com.iot.controller.repository;

import com.iot.controller.domain.AnalyticsEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsDataRepository extends ReactiveMongoRepository<AnalyticsEntity, String> {
}
