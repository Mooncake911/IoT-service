package com.iot.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GatewayErrorWebExceptionHandler Tests")
class GatewayErrorWebExceptionHandlerTest {

    private final GatewayErrorWebExceptionHandler handler =
            new GatewayErrorWebExceptionHandler(new ObjectMapper());

    private static MockServerWebExchange exchange(String path, String traceId) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (traceId != null) {
            builder.header("X-Gateway-Trace-Id", traceId);
        }
        return MockServerWebExchange.from(builder);
    }

    private static String body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }

    @Test
    @DisplayName("ResponseStatusException maps to its status with trace id")
    void handle_shouldMapResponseStatus() {
        MockServerWebExchange exchange = exchange("/api/v1/analytics/status", "trace-123");

        StepVerifier.create(handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "nope")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String json = body(exchange);
        assertThat(json).contains("\"status\":404");
        assertThat(json).contains("\"traceId\":\"trace-123\"");
        assertThat(json).contains("\"path\":\"/api/v1/analytics/status\"");
        assertThat(json).contains("nope");
    }

    @Test
    @DisplayName("generic exception maps to 500 with n/a trace id")
    void handle_shouldMapGenericTo500() {
        MockServerWebExchange exchange = exchange("/api/x", null);

        StepVerifier.create(handler.handle(exchange, new IllegalStateException("boom")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        String json = body(exchange);
        assertThat(json).contains("\"status\":500");
        assertThat(json).contains("\"traceId\":\"n/a\"");
        assertThat(json).contains("boom");
    }

    @Test
    @DisplayName("blank trace id falls back to n/a, null message to default text")
    void handle_shouldFallbackTraceAndMessage() {
        MockServerWebExchange exchange = exchange("/api/x", "   ");

        StepVerifier.create(handler.handle(exchange, new RuntimeException()))
                .verifyComplete();

        String json = body(exchange);
        assertThat(json).contains("\"traceId\":\"n/a\"");
        assertThat(json).contains("Unexpected gateway error");
    }

    @Test
    @DisplayName("unresolvable status falls back to 500")
    void handle_shouldFallbackUnknownStatus() {
        MockServerWebExchange exchange = exchange("/api/x", null);
        ResponseStatusException exotic = new ResponseStatusException(
                org.springframework.http.HttpStatusCode.valueOf(599), "exotic");

        StepVerifier.create(handler.handle(exchange, exotic))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("committed response rethrows original error")
    void handle_shouldRethrowWhenCommitted() {
        org.springframework.http.server.reactive.ServerHttpResponse response =
                mock(org.springframework.http.server.reactive.ServerHttpResponse.class);
        when(response.isCommitted()).thenReturn(true);
        org.springframework.web.server.ServerWebExchange exchange =
                mock(org.springframework.web.server.ServerWebExchange.class);
        when(exchange.getResponse()).thenReturn(response);
        RuntimeException failure = new RuntimeException("late");

        StepVerifier.create(handler.handle(exchange, failure))
                .expectErrorMessage("late")
                .verify();
    }

    @Test
    @DisplayName("broken ObjectMapper falls back to hand-made json")
    void handle_shouldFallbackWhenMapperFails() throws Exception {
        ObjectMapper broken = mock(ObjectMapper.class);
        when(broken.writeValueAsString(any())).thenThrow(
                new com.fasterxml.jackson.core.JsonProcessingException("broken") {
                });
        GatewayErrorWebExceptionHandler fallbackHandler = new GatewayErrorWebExceptionHandler(broken);
        MockServerWebExchange exchange = exchange("/api/x", "t-1");

        StepVerifier.create(fallbackHandler.handle(exchange, new RuntimeException("x")))
                .verifyComplete();

        String json = body(exchange);
        assertThat(json).contains("\"status\":500");
        assertThat(json).contains("\"traceId\":\"t-1\"");
    }
}
