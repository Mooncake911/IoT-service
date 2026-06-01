package com.iot.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";
    private static final String TRACE_ID = "X-Gateway-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(TRACE_ID, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getResponse().getHeaders().set(TRACE_ID, traceId);

        log.info("[{}] Incoming request: {} {}", traceId, request.getMethod(), request.getURI().getPath());

        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            Long startTime = mutatedExchange.getAttribute(START_TIME);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("[{}] Outgoing response: {} ({}ms)",
                        traceId, mutatedExchange.getResponse().getStatusCode(), duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
