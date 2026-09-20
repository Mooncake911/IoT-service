package com.iot.alerts.controller;

import com.iot.alerts.controller.dto.RuleResponse;
import com.iot.alerts.controller.dto.RuleUpsertRequest;
import com.iot.alerts.model.RuleType;
import com.iot.alerts.model.Severity;
import com.iot.alerts.service.RuleManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(RuleController.class)
@DisplayName("RuleController Tests")
class RuleControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RuleManagementService ruleManagementService;

    private static RuleUpsertRequest request() {
        return new RuleUpsertRequest("low-battery", RuleType.INSTANT, Severity.WARNING,
                "BATTERY_LEVEL", "LT", 20.0, null, null, 1, 30, true);
    }

    private static RuleResponse response(String id) {
        Instant now = Instant.now();
        return new RuleResponse(id, "low-battery", RuleType.INSTANT, Severity.WARNING,
                "BATTERY_LEVEL", "LT", 20.0, null, null, 1, 30, true, now, now);
    }

    @Test
    @DisplayName("GET /api/alerts/rules returns all rules")
    void getRules_shouldReturnAll() {
        when(ruleManagementService.listRules()).thenReturn(Flux.just(response("1"), response("2")));

        webTestClient.get()
                .uri("/api/alerts/rules")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("1")
                .jsonPath("$[1].id").isEqualTo("2");

        verify(ruleManagementService).listRules();
    }

    @Test
    @DisplayName("POST /api/alerts/rules creates rule with 201")
    void createRule_shouldReturnCreated() {
        when(ruleManagementService.createRule(any(RuleUpsertRequest.class)))
                .thenReturn(Mono.just(response("10")));

        webTestClient.post()
                .uri("/api/alerts/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("10");

        verify(ruleManagementService).createRule(any(RuleUpsertRequest.class));
    }

    @Test
    @DisplayName("PUT /api/alerts/rules/{id} updates rule")
    void updateRule_shouldReturnUpdated() {
        when(ruleManagementService.updateRule(eq("10"), any(RuleUpsertRequest.class)))
                .thenReturn(Mono.just(response("10")));

        webTestClient.put()
                .uri("/api/alerts/rules/10")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("10");

        verify(ruleManagementService).updateRule(eq("10"), any(RuleUpsertRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/alerts/rules/{id} returns 204")
    void deleteRule_shouldReturnNoContent() {
        when(ruleManagementService.deleteRule("10")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/alerts/rules/10")
                .exchange()
                .expectStatus().isNoContent();

        verify(ruleManagementService).deleteRule("10");
    }

    @Test
    @DisplayName("service IllegalArgumentException maps to 400")
    void createRule_whenInvalid_shouldReturnBadRequest() {
        when(ruleManagementService.createRule(any(RuleUpsertRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Numeric field requires thresholdNumber only")));

        webTestClient.post()
                .uri("/api/alerts/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("bad_request")
                .jsonPath("$.message").isEqualTo("Numeric field requires thresholdNumber only");
    }
}
