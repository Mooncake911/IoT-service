package com.iot.analytics.controller;

import com.iot.analytics.service.AnalyticsService;
import com.iot.shared.domain.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Slf4j
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/data")
    public Mono<Void> receiveData(@RequestBody List<DeviceData> deviceData) {
        log.debug("Received analytics data batch: size={}", deviceData.size());
        return analyticsService.calculateAndPublishStats(deviceData);
    }

    @PostMapping("/config")
    public Mono<String> setConfig(@RequestParam String method, @RequestParam int batchSize) {
        log.info("Updating analytics config: method={}, batchSize={}", method, batchSize);
        analyticsService.setCalculationMethod(method, batchSize);
        return Mono.just("Calculation method switched to: " + method + " (batch size: " + batchSize + ")");
    }

    @GetMapping("/status")
    public Mono<java.util.Map<String, Object>> getStatus() {
        return Mono.just(analyticsService.getConfiguration());
    }
}
