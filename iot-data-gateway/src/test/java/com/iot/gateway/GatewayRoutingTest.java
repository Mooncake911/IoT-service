package com.iot.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("Gateway Health Check should be UP")
    void healthCheck_shouldReturnUp() {
        webClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("API docs endpoint should expose links map")
    void apiDocs_shouldReturnLinks() {
        webClient.get().uri("/api/docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiVersion").isEqualTo("v1")
                .jsonPath("$.docs.gateway").isEqualTo("/v3/api-docs");
    }

    @Test
    @DisplayName("API v1 docs endpoint should expose links map")
    void apiDocsV1_shouldReturnLinks() {
        webClient.get().uri("/api/v1/docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiVersion").isEqualTo("v1")
                .jsonPath("$.docs.gateway").isEqualTo("/v3/api-docs");
    }

    @Test
    @DisplayName("Should have all 4 routes configured")
    void routes_shouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();
        assertThat(routes).hasSize(4);
        assertThat(routes).anyMatch(r -> r.getId().equals("iot-analytics"));
        assertThat(routes).anyMatch(r -> r.getId().equals("iot-alerts"));
        assertThat(routes).anyMatch(r -> r.getId().equals("iot-data-simulator"));
        assertThat(routes).anyMatch(r -> r.getId().equals("iot-controller"));
    }
}
