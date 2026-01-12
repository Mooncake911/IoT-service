package com.iot.analytics.controller;

import com.iot.analytics.service.AnalyticsService;
import com.iot.shared.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@WebFluxTest(AnalyticsController.class)
@DisplayName("Analytics Controller Tests")
public class AnalyticsControllerTest {

        @Autowired
        private WebTestClient webTestClient;

        @MockitoBean
        private AnalyticsService analyticsService;

        @Test
        public void receiveData_shouldCallCalculateAndPublishStats() {
                List<DeviceData> deviceData = Collections.singletonList(new DeviceData(1L, "Test", null, null, null, null, null));

                webTestClient.post()
                                .uri("/api/analytics/data")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(deviceData)
                                .exchange()
                                .expectStatus().isOk();

                verify(analyticsService).calculateAndPublishStats(any());
        }

        @Test
        public void setConfig_shouldUpdateCalculationMethod() {
                String method = "Flowable";
                int batchSize = 25;

                webTestClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/analytics/config")
                                                .queryParam("method", method)
                                                .queryParam("batchSize", batchSize)
                                                .build())
                                .exchange()
                                .expectStatus().isOk();

                verify(analyticsService).setCalculationMethod(eq(method), eq(batchSize));
        }
}
