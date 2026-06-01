package com.iot.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayAnalyticsRewriteTest {

    private static HttpServer analyticsStub;
    private static String analyticsUrl;
    private static final AtomicReference<String> capturedPath = new AtomicReference<>();
    private static final AtomicReference<String> capturedMethod = new AtomicReference<>();

    @Autowired
    private WebTestClient webClient;

    @BeforeAll
    static void startStubAnalytics() throws IOException {
        analyticsStub = HttpServer.create(new InetSocketAddress(0), 0);
        analyticsStub.createContext("/api/analytics/status", new StubHandler());
        analyticsStub.createContext("/api/analytics/live/summary", new StubHandler());
        analyticsStub.createContext("/api/analytics/report/window", new StubHandler());
        analyticsStub.start();
        int port = analyticsStub.getAddress().getPort();
        analyticsUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void stopStubAnalytics() {
        if (analyticsStub != null) {
            analyticsStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void overrideAnalyticsUrl(DynamicPropertyRegistry registry) {
        registry.add("ANALYTICS_URL", () -> analyticsUrl);
    }

    @Test
    @DisplayName("Gateway should rewrite /api/v1/analytics/status to /api/analytics/status")
    void gatewayShouldRewriteAnalyticsPathV1() {
        webClient.get()
                .uri("/api/v1/analytics/status")
                .exchange()
                .expectStatus().isOk();

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedPath.get()).isEqualTo("/api/analytics/status");
    }

    @Test
    @DisplayName("Gateway should rewrite /api/v1/analytics/live/summary to /api/analytics/live/summary")
    void gatewayShouldRewriteAnalyticsLiveSummaryPathV1() {
        webClient.get()
                .uri("/api/v1/analytics/live/summary")
                .exchange()
                .expectStatus().isOk();

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedPath.get()).isEqualTo("/api/analytics/live/summary");
    }

    @Test
    @DisplayName("Gateway should rewrite /api/v1/analytics/report/window to /api/analytics/report/window")
    void gatewayShouldRewriteAnalyticsReportWindowPathV1() {
        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/analytics/report/window")
                        .queryParam("from", "2026-06-01T00:00:00Z")
                        .queryParam("to", "2026-06-01T00:05:00Z")
                        .build())
                .exchange()
                .expectStatus().isOk();

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedPath.get()).isEqualTo("/api/analytics/report/window");
    }

    private static final class StubHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedMethod.set(exchange.getRequestMethod());
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
