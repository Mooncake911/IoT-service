package com.iot.controller.service;

import com.iot.controller.config.RabbitConfig;
import com.iot.controller.domain.DeviceEntity;
import com.iot.controller.repository.DeviceDataRepository;
import com.iot.shared.domain.Device;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private final DeviceDataRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public IngestionService(DeviceDataRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Mono<DeviceEntity> ingestData(Device device) {
        // 0. Validate data
        try {
            validateDevice(device);
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        }

        // Map DTO -> Entity
        DeviceEntity entity = new DeviceEntity(
                null, // Mongo ID auto-generated
                device.getId(),
                device.getName(),
                device.getManufacturer(),
                device.getType(),
                device.getCapabilities(),
                device.getLocation(),
                device.getStatus(),
                Instant.now());

        // 1. Save to MongoDB (Reactive)
        return repository.save(entity)
                .doOnSuccess(savedEntity -> {
                    // 2. Publish original DTO to RabbitMQ (Analytics expects 'Device')
                    // We run this on a separate scheduler
                    Mono.fromRunnable(() -> rabbitTemplate.convertAndSend(RabbitConfig.DATA_EXCHANGE_NAME, "", device))
                            .subscribeOn(Schedulers.boundedElastic()).subscribe();
                });
    }

    private void validateDevice(Device device) {
        validateNotNull(device, "Device");
        validateNonNegative(device.getId(), "Device ID");
        validateStringNotEmpty(device.getName(), "Device Name");

        if (device.getStatus() != null) {
            validateBatteryRange(device.getStatus().batteryLevel(), "Battery Level");
            validateSignalRange(device.getStatus().signalStrength(), "Signal Strength");
        }

        validateLocation(device.getLocation());
        validateCapabilities(device.getCapabilities());
    }

    private void validateLocation(com.iot.shared.domain.components.Location location) {
        if (location != null) {
            // Assuming 2D/3D workspace bounds, for example -5000 to 5000
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

    private void validateMinMaxConsistency(double min, double max, String dataType) {
        if (min > max) {
            throw new IllegalArgumentException(
                    dataType + " min value (" + min + ") cannot be greater than max value (" + max + ")");
        }
    }

    private void validateGroupingMap(Map<?, Long> map, String fieldName) {
        if (map != null) {
            for (Map.Entry<?, Long> entry : map.entrySet()) {
                if (entry.getValue() <= 0) {
                    throw new IllegalArgumentException(
                            fieldName + " contains non-positive count for key " + entry.getKey() + ": "
                                    + entry.getValue());
                }
            }
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
