package com.iot.controller.controller;

import com.iot.contracts.domain.DeviceData;
import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;
import com.iot.controller.service.IngestionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest({IngestionController.class, GlobalExceptionHandler.class})
class IngestionControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private IngestionService ingestionService;

    @Test
    @DisplayName("Should accept valid batch")
    void ingestBatch_shouldReturnAccepted() {
        DeviceData device = DeviceData.builder()
                .id(1L)
                .name("Test Device")
                .manufacturer("Test")
                .type(Type.SENSOR_TEMPERATURE)
                .location(new Location(10, 20, 0))
                .status(new Status(true, 50, 80, Instant.now()))
                .build();

        when(ingestionService.ingestBatch(anyList())).thenReturn(Mono.empty());

        webClient.post()
                .uri("/api/ingest")
                .bodyValue(List.of(device))
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    @DisplayName("Should return 400 with details for ConstraintViolationException")
    void ingestBatch_shouldReturnBadRequest_withDetails() {
        // Arrange
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<DeviceData> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        
        when(path.toString()).thenReturn("status.batteryLevel");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be <= 100");
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));

        when(ingestionService.ingestBatch(anyList())).thenReturn(Mono.error(ex));

        DeviceData invalidDevice = DeviceData.builder().id(1L).build();

        // Act & Assert
        webClient.post()
                .uri("/api/ingest")
                .bodyValue(List.of(invalidDevice))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Validation failed")
                .jsonPath("$.details['status.batteryLevel']").isEqualTo("must be <= 100");
    }

    @Test
    @DisplayName("Should return 400 for generic IllegalArgumentException")
    void ingestBatch_shouldReturnBadRequest_whenIllegalArgument() {
        when(ingestionService.ingestBatch(anyList()))
            .thenReturn(Mono.error(new IllegalArgumentException("Invalid input")));

        DeviceData device = DeviceData.builder().id(1L).build();

        webClient.post()
                .uri("/api/ingest")
                .bodyValue(List.of(device))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Invalid input");
    }
}
