package com.iot.simulator.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient.Builder webClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        simulationService = new SimulationService(webClientBuilder, 10, 1, 5);
        ReflectionTestUtils.setField(simulationService, "controllerUrl", "http://localhost:8080");

        // Default mock behavior for WebClient
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Void.class))
                .thenReturn(Mono.empty());
    }

    @AfterEach
    void tearDown() {
        simulationService.stop().block();
    }

    @Test
    @DisplayName("Should initialize with default values")
    void shouldInitialize() {
        assertThat(simulationService.getDeviceCount()).isEqualTo(10);
        assertThat(simulationService.getFrequencySeconds()).isEqualTo(1);
        assertThat(simulationService.getBatchSize()).isEqualTo(5);
        assertThat(simulationService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("Should update configuration and generate devices")
    void configure_shouldUpdateValuesAndDevices() {
        StepVerifier.create(simulationService.configure(5, 2))
                .verifyComplete();

        assertThat(simulationService.getDeviceCount()).isEqualTo(5);
        assertThat(simulationService.getFrequencySeconds()).isEqualTo(2);

        Object deviceData = ReflectionTestUtils.getField(simulationService, "deviceData");
        assertThat(deviceData).isNotNull();
    }

    @Test
    @DisplayName("Should start and stop simulation via Sink")
    void startAndStop_shouldChangeState() {
        StepVerifier.create(simulationService.start())
                .verifyComplete();
        assertThat(simulationService.isRunning()).isTrue();

        StepVerifier.create(simulationService.stop())
                .verifyComplete();
        assertThat(simulationService.isRunning()).isFalse();
    }

    @Test
    @DisplayName("Should send data to backend when running")
    void simulation_shouldSendData() throws InterruptedException {
        simulationService.configure(2, 1).block();
        simulationService.start().block();

        // Wait for at least one interval tick
        Thread.sleep(1500);

        verify(webClient.post(), atLeastOnce()).uri(anyString());

        simulationService.stop().block();
    }

    @Test
    @DisplayName("Should handle backend errors gracefully")
    void simulation_shouldHandleErrors() throws InterruptedException {
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Void.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")))
                .thenReturn(Mono.empty());

        simulationService.configure(1, 1).block();
        simulationService.start().block();

        Thread.sleep(2000);

        verify(webClient.post(), atLeastOnce()).uri(anyString());

        simulationService.stop().block();
    }
}
