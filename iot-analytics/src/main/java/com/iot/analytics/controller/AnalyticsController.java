package com.iot.analytics.controller;

import com.iot.analytics.service.AnalyticsService;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.shared.domain.Device;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/data")
    public Mono<Void> receiveData(@RequestBody java.util.List<Device> devices) {
        return analyticsService.processData(Flux.fromIterable(devices));
    }

    @GetMapping("/stats")
    public Mono<DeviceStats> getStats(
            @RequestParam(name = "method", defaultValue = "Observable") String method,
            @RequestParam(name = "batchSize", defaultValue = "50") int batchSize) {
        return analyticsService.getCurrentStats(method, batchSize);
    }
}
