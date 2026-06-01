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
class GatewayControllerRewriteTest {

    private static HttpServer controllerStub;
    private static String controllerUrl;
    private static final AtomicReference<String> capturedPath = new AtomicReference<>();
    private static final AtomicReference<String> capturedMethod = new AtomicReference<>();

    @Autowired
    private WebTestClient webClient;

    @BeforeAll
    static void startStubController() throws IOException {
        controllerStub = HttpServer.create(new InetSocketAddress(0), 0);
        controllerStub.createContext("/api/ingest", new StubHandler());
        controllerStub.start();
        int port = controllerStub.getAddress().getPort();
        controllerUrl = "http://localhost:" + port;
    }

    @AfterAll
    static void stopStubController() {
        if (controllerStub != null) {
            controllerStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void overrideControllerUrl(DynamicPropertyRegistry registry) {
        registry.add("CONTROLLER_URL", () -> controllerUrl);
    }

    @Test
    @DisplayName("Gateway should rewrite /api/controller to /api/ingest")
    void gatewayShouldRewriteControllerPath() {
        webClient.post()
                .uri("/api/controller")
                .exchange()
                .expectStatus().isAccepted();

        assertThat(capturedMethod.get()).isEqualTo("POST");
        assertThat(capturedPath.get()).isIn("/api/ingest", "/api/ingest/");
    }

    @Test
    @DisplayName("Gateway should rewrite /api/v1/controller to /api/ingest")
    void gatewayShouldRewriteControllerPathV1() {
        webClient.post()
                .uri("/api/v1/controller")
                .exchange()
                .expectStatus().isAccepted();

        assertThat(capturedMethod.get()).isEqualTo("POST");
        assertThat(capturedPath.get()).isIn("/api/ingest", "/api/ingest/");
    }

    private static final class StubHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedMethod.set(exchange.getRequestMethod());
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(202, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }
}
