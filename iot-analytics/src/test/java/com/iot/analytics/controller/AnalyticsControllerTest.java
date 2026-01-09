package com.iot.analytics.controller;

import com.iot.analytics.service.AnalyticsService;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.shared.domain.Device;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AnalyticsController.class)
@DisplayName("Analytics Controller Tests")
public class AnalyticsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    public void receiveData_shouldCallProcessData() {
        List<Device> devices = Collections.singletonList(new Device());

        when(analyticsService.processData(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/analytics/data")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(devices)
                .exchange()
                .expectStatus().isOk();

        verify(analyticsService).processData(any());
    }

    @Test
    public void getStats_shouldPassParametersToService() {
        String method = "Reactive";
        int batchSize = 100;

        when(analyticsService.getCurrentStats(eq(method), eq(batchSize)))
                .thenReturn(Mono.just(DeviceStats.empty()));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/analytics/stats")
                        .queryParam("method", method)
                        .queryParam("batchSize", batchSize)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(analyticsService).getCurrentStats(eq(method), eq(batchSize));
    }
}
