package com.iot.analytics.controller;

import com.iot.analytics.domain.AnalyticsEntity;
import com.iot.analytics.repository.AnalyticsDataRepository;
import com.iot.analytics.service.AnalyticsService;
import com.iot.analytics.service.LiveAnalyticsService;
import com.iot.analytics.service.ReportAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AnalyticsController.class)
@DisplayName("Analytics history/report/status endpoint Tests")
class AnalyticsHistoryEndpointsTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private AnalyticsDataRepository analyticsDataRepository;

    @MockitoBean
    private LiveAnalyticsService liveAnalyticsService;

    @MockitoBean
    private ReportAnalyticsService reportAnalyticsService;

    @Test
    @DisplayName("GET /api/analytics/status returns configuration")
    void getStatus_shouldReturnConfiguration() {
        when(analyticsService.getConfiguration()).thenReturn(Map.of("method", "Parallel", "windowSeconds", 30));

        webTestClient.get()
                .uri("/api/analytics/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.method").isEqualTo("Parallel");

        verify(analyticsService).getConfiguration();
    }

    @Test
    @DisplayName("GET /api/analytics/history returns entities")
    void getHistory_shouldReturnEntities() {
        AnalyticsEntity entity = new AnalyticsEntity("h1", Instant.now(), Map.of("totalDevices", 5.0));
        when(analyticsDataRepository.findAllByOrderByTimestampDesc(any(Pageable.class)))
                .thenReturn(Flux.just(entity));

        webTestClient.get()
                .uri("/api/analytics/history?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("h1");

        verify(analyticsDataRepository).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/analytics/report/window delegates to service")
    void getWindowReport_shouldDelegate() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        when(reportAnalyticsService.windowReport(eq(from), eq(to)))
                .thenReturn(Mono.just(Map.of("windows", 3)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/analytics/report/window")
                        .queryParam("from", "2026-01-01T00:00:00Z")
                        .queryParam("to", "2026-01-02T00:00:00Z")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.windows").isEqualTo(3);

        verify(reportAnalyticsService).windowReport(eq(from), eq(to));
    }

    @Test
    @DisplayName("GET /api/analytics/live/summary returns summary")
    void getLiveSummary_shouldReturnSummary() {
        when(liveAnalyticsService.getSummary()).thenReturn(Map.of("totalUniqueDevices", 7L));

        webTestClient.get()
                .uri("/api/analytics/live/summary")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalUniqueDevices").isEqualTo(7);

        verify(liveAnalyticsService).getSummary();
    }
}
