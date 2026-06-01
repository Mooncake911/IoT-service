package com.iot.controller.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.controller.domain.DeviceEntity;
import com.iot.controller.repository.DeviceDataRepository;
import com.iot.controller.validation.DeviceValidator;
import com.iot.contracts.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class IngestionService {

    private final DeviceDataRepository repository;
    private final Sender sender;
    private final DeviceValidator validator;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.exchange.data}")
    private String dataExchangeName;

    @Value("${app.rabbitmq.chunk-size}")
    private int publishChunkSize;

    public IngestionService(DeviceDataRepository repository, Sender sender, DeviceValidator validator, ObjectMapper objectMapper) {
        this.repository = repository;
        this.sender = sender;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> ingestBatch(List<DeviceData> deviceData) {
        return Mono.fromCallable(() -> {
            deviceData.forEach(validator::validate);
            return deviceData.stream().map(this::toEntity).toList();
        }).flatMap(entities -> repository.saveAll(entities)
                .then(publishInChunksReactive(deviceData))
                .doOnSuccess(saved -> log.info("Batch ingested: size={}", deviceData.size()))
                .doOnError(error -> log.error("Failed to ingest batch: {}", error.getMessage())))
                .then();
    }

    private Mono<Void> publishInChunksReactive(List<DeviceData> deviceData) {
        return Flux.fromIterable(deviceData)
                .map(data -> new OutboundMessage(dataExchangeName, "", serialize(data)))
                .window(publishChunkSize)
                .flatMap(chunk -> sender.send(chunk))
                .then();
    }

    private byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
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
}
