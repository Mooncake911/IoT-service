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
class GatewaySimulatorRewriteTest {

    private static HttpServer simulatorStub;
    private static String simulatorUrl;
    private static final AtomicReference<String> capturedPath = new AtomicReference<>();
    private static final AtomicReference<String> capturedMethod = new AtomicReference<>();

    @Autowired
    private WebTestClient webClient;

    @BeforeAll
    static void startStubSimulator() throws IOException {
        simulatorStub = HttpServer.create(new InetSocketAddress(0), 0);
        simulatorStub.createContext("/api/simulator/status", new StubHandler());
        simulatorStub.start();
        int port = simulatorStub.getAddress().getPort();
        simulatorUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void stopStubSimulator() {
        if (simulatorStub != null) {
            simulatorStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void overrideSimulatorUrl(DynamicPropertyRegistry registry) {
        registry.add("SIMULATOR_URL", () -> simulatorUrl);
    }

    @Test
    @DisplayName("Gateway should rewrite /api/v1/simulator/status to /api/simulator/status")
    void gatewayShouldRewriteSimulatorPathV1() {
        webClient.get()
                .uri("/api/v1/simulator/status")
                .exchange()
                .expectStatus().isOk();

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedPath.get()).isEqualTo("/api/simulator/status");
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
