package com.iot.controller.service;

import com.iot.controller.config.RabbitConfig;
import com.iot.controller.domain.DeviceEntity;
import com.iot.controller.repository.DeviceDataRepository;
import com.iot.shared.domain.Device;
import com.iot.shared.domain.components.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionServiceTest {

    @Mock
    private DeviceDataRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private IngestionService ingestionService;

    @Test
    @DisplayName("Should save to Mongo and publish to RabbitMQ")
    public void ingestData_shouldSaveAndPublish() {
        // Arrange
        Device device = new Device();
        device.setId(123L);
        device.setName("Test Device");
        device.setType(Type.CAMERA);
        device.setCapabilities(Collections.singletonList("temp"));

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
        Mono<DeviceEntity> result = ingestionService.ingestData(device);

        // Assert
        StepVerifier.create(result)
                .expectNext(savedEntity)
                .verifyComplete();

        // Verify repository save called
        verify(repository).save(any(DeviceEntity.class));

        // Verify RabbitMQ publish (it happens async in doOnSuccess)
        // We use timeout() to wait for the fire-and-forget subscription to execute
        verify(rabbitTemplate, timeout(1000)).convertAndSend(eq(RabbitConfig.DATA_EXCHANGE_NAME), eq(""), eq(device));
    }
}
