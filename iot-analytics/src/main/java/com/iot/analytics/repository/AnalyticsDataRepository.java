package com.iot.analytics.repository;

import com.iot.analytics.domain.AnalyticsEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface AnalyticsDataRepository extends ReactiveMongoRepository<AnalyticsEntity, String> {

    Flux<AnalyticsEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    Flux<AnalyticsEntity> findAllByTimestampBetweenOrderByTimestampAsc(Instant from, Instant to);
}

