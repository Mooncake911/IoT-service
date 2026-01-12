package com.iot.controller.controller;

import com.iot.controller.service.IngestionService;
import com.iot.shared.domain.DeviceData;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(IngestionController.class)
@DisplayName("Ingestion Controller API Test")
public class IngestionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private IngestionService ingestionService;

    @Test
    @DisplayName("POST /api/ingest should call ingestionService.ingestBatch")
    void ingestBatch_shouldCallService() {
        List<DeviceData> deviceData = Collections.singletonList(new DeviceData(1L, "Test", null, null, null, null, null));

        when(ingestionService.ingestBatch(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(deviceData)
                .exchange()
                .expectStatus().isAccepted();

        verify(ingestionService).ingestBatch(any());
    }
}
