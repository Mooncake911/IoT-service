package com.iot.analytics.controller;

import com.iot.analytics.service.AnalyticsService;
import com.iot.analytics.service.LiveAnalyticsService;
import com.iot.analytics.service.ReportAnalyticsService;
import com.iot.contracts.domain.AnalyticsData;
import com.iot.contracts.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@WebFluxTest(AnalyticsController.class)
@DisplayName("Analytics Controller Tests")
public class AnalyticsControllerTest {

        @Autowired
        private WebTestClient webTestClient;

        @MockitoBean
        private AnalyticsService analyticsService;

        @MockitoBean
        private com.iot.analytics.repository.AnalyticsDataRepository analyticsDataRepository;

        @MockitoBean
        private LiveAnalyticsService liveAnalyticsService;

        @MockitoBean
        private ReportAnalyticsService reportAnalyticsService;

        @Test
        public void getLiveByManufacturer_shouldReturnManufacturerDistribution() {
                when(liveAnalyticsService.getByManufacturer()).thenReturn(Map.of(
                                "timestamp", "2026-06-03T19:45:54Z",
                                "manufacturers", Map.of("Acme", 10, "Omni", 5)));

                webTestClient.get()
                                .uri("/api/analytics/live/by-manufacturer")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.manufacturers.Acme").isEqualTo(10)
                                .jsonPath("$.manufacturers.Omni").isEqualTo(5);

                verify(liveAnalyticsService).getByManufacturer();
        }

        @Test
        public void receiveData_shouldCallCalculateAndPublishStats() {
                List<DeviceData> deviceData = List.of(new DeviceData(1L, "Test", null, null, null, null, null));
                when(analyticsService.calculateStats(any())).thenReturn(Mono.just(AnalyticsData.builder()
                        .timestamp(java.time.Instant.now())
                        .metrics(Map.of("totalDevices", 1.0))
                        .build()));

                webTestClient.post()
                                .uri("/api/analytics/data")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(deviceData)
                                .exchange()
                                .expectStatus().isOk();

                verify(analyticsService).calculateStats(any());
        }

        @Test
        public void setConfig_shouldUpdateCalculationMethod() {
                String method = "Parallel";
                int windowSeconds = 25;

                webTestClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/analytics/config")
                                                .queryParam("method", method)
                                                .queryParam("windowSeconds", windowSeconds)
                                                .build())
                                .exchange()
                                .expectStatus().isOk();

                verify(analyticsService).setCalculationMethod(eq(method), eq(windowSeconds));
        }
}

