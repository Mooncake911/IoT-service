package com.iot.controller.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.contracts.domain.DeviceData;
import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;
import com.iot.controller.domain.DeviceEntity;
import com.iot.controller.repository.DeviceDataRepository;
import com.iot.controller.validation.DeviceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Sender;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionServiceTest {

    @Mock
    private DeviceDataRepository repository;

    @Mock
    private Sender sender;

    @Mock
    private DeviceValidator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private IngestionService ingestionService;

    @BeforeEach
    public void setUp() {
        ingestionService = new IngestionService(repository, sender, validator, objectMapper);
        ReflectionTestUtils.setField(ingestionService, "dataExchangeName", "iot.data.exchange");
        ReflectionTestUtils.setField(ingestionService, "publishChunkSize", 50);
    }

    @Test
    @DisplayName("Should save batch to Mongo and publish to RabbitMQ via Sender")
    public void ingestBatch_shouldSaveAndPublish() {
        // Arrange
        DeviceData deviceData = DeviceData.builder()
                .id(123L)
                .name("Test Device")
                .manufacturer("Acme")
                .type(Type.SENSOR_TEMPERATURE)
                .capabilities(Collections.singletonList("temp"))
                .location(new Location(1, 2, 0))
                .status(new Status(true, 80, 70, Instant.now()))
                .build();

        DeviceEntity entity = new DeviceEntity(
                null,
                123L,
                "Test Device",
                "Acme",
                Type.SENSOR_TEMPERATURE,
                Collections.singletonList("temp"),
                new Location(1, 2, 0),
                new Status(true, 80, 70, Instant.now()),
                Instant.now());

        when(repository.saveAll(any(List.class))).thenReturn(Flux.just(entity));
        when(sender.send(any(Flux.class))).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = ingestionService.ingestBatch(Collections.singletonList(deviceData));

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        // Verify repository batch save called
        verify(repository).saveAll(any(List.class));

        // Verify RabbitMQ publish via sender
        verify(sender, timeout(1000)).send(any(Flux.class));
    }
}
