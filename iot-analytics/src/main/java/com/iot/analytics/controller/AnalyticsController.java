package com.iot.analytics.controller;

import com.iot.analytics.domain.AnalyticsEntity;
import com.iot.analytics.repository.AnalyticsDataRepository;
import com.iot.analytics.service.AnalyticsService;
import com.iot.analytics.service.LiveAnalyticsService;
import com.iot.analytics.service.ReportAnalyticsService;
import com.iot.contracts.domain.AnalyticsData;
import com.iot.contracts.domain.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@Slf4j
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsDataRepository analyticsDataRepository;
    private final LiveAnalyticsService liveAnalyticsService;
    private final ReportAnalyticsService reportAnalyticsService;

    @PostMapping("/data")
    public Mono<AnalyticsData> receiveData(@RequestBody List<DeviceData> deviceData) {
        log.debug("Received analytics data batch: size={}", deviceData.size());
        return analyticsService.calculateStats(deviceData);
    }

    @PostMapping("/config")
    public Mono<String> setConfig(@RequestParam("method") String method, @RequestParam("batchSize") int batchSize) {
        log.info("Updating analytics config: method={}, batchSize={}", method, batchSize);
        analyticsService.setCalculationMethod(method, batchSize);
        return Mono.just("Calculation method switched to: " + method + " (batch size: " + batchSize + ")");
    }

    @GetMapping("/status")
    public Mono<java.util.Map<String, Object>> getStatus() {
        return Mono.just(analyticsService.getConfiguration());
    }

    @GetMapping("/live/summary")
    public Mono<Map<String, Object>> getLiveSummary() {
        return Mono.just(liveAnalyticsService.getSummary());
    }

    @GetMapping("/live/by-type")
    public Mono<Map<String, Object>> getLiveByType() {
        return Mono.just(liveAnalyticsService.getByType());
    }

    @GetMapping("/history")
    public Flux<AnalyticsEntity> getHistory(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        log.debug("Fetching analytics history, limit={}", limit);
        return analyticsDataRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit));
    }

    @GetMapping("/report/window")
    public Mono<Map<String, Object>> getWindowReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportAnalyticsService.windowReport(from, to);
    }
}

