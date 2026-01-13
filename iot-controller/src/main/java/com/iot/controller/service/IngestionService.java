package com.iot.controller.service;

import com.iot.controller.domain.DeviceEntity;
import com.iot.controller.repository.DeviceDataRepository;
import com.iot.shared.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class IngestionService {

    private final DeviceDataRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.data}")
    private String dataExchangeName;

    public IngestionService(DeviceDataRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Mono<DeviceEntity> ingestData(DeviceData deviceData) {
        return Mono.fromCallable(() -> {
            validateDevice(deviceData);
            return toEntity(deviceData);
        }).flatMap(entity -> repository.save(entity)
                .flatMap(savedEntity -> Mono
                        .fromRunnable(() -> rabbitTemplate.convertAndSend(dataExchangeName, "", deviceData))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(savedEntity))
                .doOnSuccess(saved -> log.debug("Data ingested for device {}", deviceData.id()))
                .doOnError(error -> log.error("Failed to ingest data for device {}: {}", deviceData.id(),
                        error.getMessage())));
    }

    public Mono<Void> ingestBatch(List<DeviceData> deviceData) {
        return Mono.fromCallable(() -> {
            deviceData.forEach(this::validateDevice);
            return deviceData.stream().map(this::toEntity).toList();
        }).flatMap(entities -> repository.saveAll(entities).collectList()
                .flatMap(savedEntities -> Mono
                        .fromRunnable(() -> rabbitTemplate.convertAndSend(dataExchangeName, "", deviceData))
                        .subscribeOn(Schedulers.boundedElastic()))
                .doOnSuccess(saved -> log.info("Batch ingested: size={}", deviceData.size()))
                .doOnError(error -> log.error("Failed to ingest batch: {}", error.getMessage())))
                .then();
    }

    private DeviceEntity toEntity(DeviceData deviceData) {
        return new DeviceEntity(
                null,
                deviceData.id(),
                deviceData.name(),
                deviceData.manufacturer(),
                deviceData.type(),
                deviceData.capabilities(),
                deviceData.location(),
                deviceData.status(),
                Instant.now());
    }

    private void validateDevice(DeviceData deviceData) {
        validateNotNull(deviceData, "Device");
        validateNonNegative(deviceData.id(), "Device ID");
        validateStringNotEmpty(deviceData.name(), "Device Name");

        if (deviceData.status() != null) {
            validateBatteryRange(deviceData.status().batteryLevel(), "Battery Level");
            validateSignalRange(deviceData.status().signalStrength(), "Signal Strength");
        }

        validateLocation(deviceData.location());
        validateCapabilities(deviceData.capabilities());
    }

    private void validateLocation(com.iot.shared.domain.components.Location location) {
        if (location != null) {
            if (Math.abs(location.x()) > 10000 || Math.abs(location.y()) > 10000) {
                throw new IllegalArgumentException("Location coordinates out of range: " + location);
            }
        }
    }

    private void validateCapabilities(List<String> capabilities) {
        if (capabilities != null) {
            for (String cap : capabilities) {
                if (cap == null || cap.trim().isEmpty()) {
                    throw new IllegalArgumentException("Capability cannot be null or empty");
                }
            }
            if (capabilities.size() > 50) {
                throw new IllegalArgumentException("Too many capabilities: " + capabilities.size());
            }
        }
    }

    private void validateNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative: " + value);
        }
    }

    private void validateBatteryRange(double value, String fieldName) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100: " + value);
        }
    }

    private void validateSignalRange(double value, String fieldName) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100: " + value);
        }
    }

    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    private void validateStringNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }
}
