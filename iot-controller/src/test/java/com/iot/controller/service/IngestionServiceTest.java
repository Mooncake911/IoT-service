package com.iot.controller.service;

import com.iot.controller.domain.DeviceEntity;
import com.iot.controller.repository.DeviceDataRepository;
import com.iot.shared.domain.DeviceData;
import com.iot.shared.domain.components.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionServiceTest {

    @Mock
    private DeviceDataRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private IngestionService ingestionService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(ingestionService, "dataExchangeName", "iot.data.exchange");
    }

    @Test
    @DisplayName("Should save to Mongo and publish to RabbitMQ")
    public void ingestData_shouldSaveAndPublish() {
        // Arrange
        DeviceData deviceData = new DeviceData(123L, "Test Device", null, Type.CAMERA, Collections.singletonList("temp"), null,
                null);

        DeviceEntity savedEntity = new DeviceEntity(
                "mongo-id-1",
                123L,
                "Test Device",
                null,
                Type.CAMERA,
                Collections.singletonList("temp"),
                null,
                null,
                null);

        when(repository.save(any(DeviceEntity.class))).thenReturn(Mono.just(savedEntity));

        // Act
        Mono<DeviceEntity> result = ingestionService.ingestData(deviceData);

        // Assert
        StepVerifier.create(result)
                .expectNext(savedEntity)
                .verifyComplete();

        // Verify repository save called
        verify(repository).save(any(DeviceEntity.class));

        // Verify RabbitMQ publish
        verify(rabbitTemplate, timeout(1000)).convertAndSend(eq("iot.data.exchange"), eq(""), eq(deviceData));
    }
}
