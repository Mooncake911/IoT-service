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
class GatewayAlertsRewriteTest {

    private static HttpServer alertsStub;
    private static String alertsUrl;
    private static final AtomicReference<String> capturedPath = new AtomicReference<>();
    private static final AtomicReference<String> capturedMethod = new AtomicReference<>();

    @Autowired
    private WebTestClient webClient;

    @BeforeAll
    static void startStubAlerts() throws IOException {
        alertsStub = HttpServer.create(new InetSocketAddress(0), 0);
        alertsStub.createContext("/api/alerts", new StubHandler());
        alertsStub.start();
        int port = alertsStub.getAddress().getPort();
        alertsUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void stopStubAlerts() {
        if (alertsStub != null) {
            alertsStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void overrideAlertsUrl(DynamicPropertyRegistry registry) {
        registry.add("ALERTS_URL", () -> alertsUrl);
    }

    @Test
    @DisplayName("Gateway should rewrite /api/v1/alerts to /api/alerts")
    void gatewayShouldRewriteAlertsPathV1() {
        webClient.get()
                .uri("/api/v1/alerts")
                .exchange()
                .expectStatus().isOk();

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedPath.get()).isIn("/api/alerts", "/api/alerts/");
    }

    private static final class StubHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedMethod.set(exchange.getRequestMethod());
            byte[] body = "[]".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
