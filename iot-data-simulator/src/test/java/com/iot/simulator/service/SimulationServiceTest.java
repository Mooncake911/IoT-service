package com.iot.simulator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        simulationService = new SimulationService(webClientBuilder, 10, 1);
        ReflectionTestUtils.setField(simulationService, "analyticsUrl", "http://localhost:8080");
    }

    @Test
    @DisplayName("Should initialize with default values")
    void shouldInitialize() {
        assertThat(simulationService.getDeviceCount()).isEqualTo(10);
        assertThat(simulationService.getMessagesPerSecond()).isEqualTo(1);
        assertThat(simulationService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("Should update configuration")
    void configure_shouldUpdateValues() {
        StepVerifier.create(simulationService.configure(20, 5))
                .verifyComplete();

        assertThat(simulationService.getDeviceCount()).isEqualTo(20);
        assertThat(simulationService.getMessagesPerSecond()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should start and stop simulation")
    void startAndStop_shouldChangeState() {
        // Mock web client behavior if needed, or rely on start() just setting
        // subscription
        // start() triggers interval which might fail if webClient calls fail, but it
        // logs error.
        // We just check state here.

        StepVerifier.create(simulationService.start())
                .verifyComplete();

        assertThat(simulationService.isRunning()).isTrue();

        StepVerifier.create(simulationService.stop())
                .verifyComplete();

        assertThat(simulationService.isRunning()).isFalse();
    }
}
