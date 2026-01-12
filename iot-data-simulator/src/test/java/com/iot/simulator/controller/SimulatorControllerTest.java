package com.iot.simulator.controller;

import com.iot.simulator.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(SimulatorController.class)
@DisplayName("Simulator Controller Tests")
public class SimulatorControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        when(simulationService.configure(anyInt(), anyInt())).thenReturn(Mono.empty());
        when(simulationService.start()).thenReturn(Mono.empty());
        when(simulationService.stop()).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Should configure simulation with provided parameters")
    public void config_shouldCallConfigure() {
        int deviceCount = 20;
        int messagesPerSecond = 5;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/simulator/config")
                        .queryParam("deviceCount", deviceCount)
                        .queryParam("messagesPerSecond", messagesPerSecond)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(simulationService).configure(deviceCount, messagesPerSecond);
    }

    @Test
    @DisplayName("Should start simulation")
    public void start_shouldCallStart() {
        webTestClient.post()
                .uri("/api/simulator/start")
                .exchange()
                .expectStatus().isOk();

        verify(simulationService).start();
    }

    @Test
    @DisplayName("Should stop simulation")
    public void stop_shouldCallStop() {
        webTestClient.post()
                .uri("/api/simulator/stop")
                .exchange()
                .expectStatus().isOk();

        verify(simulationService).stop();
    }
}
