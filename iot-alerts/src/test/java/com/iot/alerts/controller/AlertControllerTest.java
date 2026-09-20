package com.iot.alerts.controller;

import com.iot.alerts.domain.AlertEntity;
import com.iot.alerts.repository.AlertDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AlertController.class)
@DisplayName("AlertController Tests")
class AlertControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AlertDataRepository alertDataRepository;

    @Test
    @DisplayName("GET /api/alerts returns latest alerts")
    void getAlerts_shouldReturnLatest() {
        AlertEntity entity = new AlertEntity("a1", 101L, "r1", "low-battery", "WARNING",
                3, 20, Instant.now(), "INSTANT", Instant.now());
        when(alertDataRepository.findAllByOrderByReceivedAtDesc(any(Pageable.class)))
                .thenReturn(Flux.just(entity));

        webTestClient.get()
                .uri("/api/alerts?limit=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].deviceId").isEqualTo(101)
                .jsonPath("$[0].ruleName").isEqualTo("low-battery");

        verify(alertDataRepository).findAllByOrderByReceivedAtDesc(any(Pageable.class));
    }
}
