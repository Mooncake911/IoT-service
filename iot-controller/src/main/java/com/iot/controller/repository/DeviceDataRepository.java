package com.iot.controller.repository;

import com.iot.controller.domain.DeviceEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceDataRepository extends ReactiveMongoRepository<DeviceEntity, String> {
}
