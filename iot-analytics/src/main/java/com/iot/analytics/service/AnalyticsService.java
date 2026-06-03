package com.iot.analytics.service;

import com.iot.analytics.statistics.DeviceStatistics;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.contracts.domain.AnalyticsData;
import com.iot.contracts.domain.DeviceData;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnalyticsService {

    private final AtomicReference<String> currentMethod;
    private final AtomicInteger currentWindowSeconds;

    private final DeviceStatistics deviceStatistics = new DeviceStatistics();
    private final Sinks.Many<Integer> windowSecondsSink;

    public AnalyticsService(
            @Value("${app.analytics.default-method}") String defaultMethod,
            @Value("${app.analytics.default-window-seconds:30}") int defaultWindowSeconds) {
        this.currentMethod = new AtomicReference<>(defaultMethod);
        this.currentWindowSeconds = new AtomicInteger(Math.max(1, defaultWindowSeconds));
        this.windowSecondsSink = Sinks.many().replay().latestOrDefault(this.currentWindowSeconds.get());
    }

    public reactor.core.publisher.Flux<Integer> getWindowDurationFlux() {
        return windowSecondsSink.asFlux();
    }

    /**
     * Calculates aggregate metrics for a time window of unique devices and returns one analytics record.
     */
    public Mono<AnalyticsData> calculateStats(List<DeviceData> deviceData) {
        String method = currentMethod.get();

        if (deviceData.isEmpty()) {
            return Mono.empty();
        }

        List<DeviceData> uniqueDevices = uniqueByDeviceId(deviceData);

        return computeStats(uniqueDevices, method)
                .map(stats -> {
                    // Создаем объект данных для аналитики
                    return AnalyticsData.builder()
                            .timestamp(Instant.now())
                            .metrics(stats.getMetrics())
                            .build();
                })
                .doOnError(error -> log.error("Error calculating stats with method {}", method, error));
    }

    private List<DeviceData> uniqueByDeviceId(List<DeviceData> deviceData) {
        LinkedHashMap<Long, DeviceData> unique = new LinkedHashMap<>();
        for (DeviceData device : deviceData) {
            if (device != null) {
                unique.put(device.id(), device);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Mono<DeviceStats> computeStats(List<DeviceData> deviceData, String method) {
        if (deviceData.isEmpty()) {
            return Mono.just(DeviceStats.builder().build());
        }

        return switch (method) {
            case "Sequential" ->
                    Mono.fromCallable(() -> deviceStatistics.computeSequential(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
            case "Parallel" ->
                    Mono.fromCallable(() -> deviceStatistics.computeParallel(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());

            default ->
                    Mono.fromCallable(() -> deviceStatistics.computeParallel(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
        };
    }

    public void setCalculationMethod(String method, int windowSeconds) {
        log.info("Switching analytics calculation method to: {}, windowSeconds: {}", method, windowSeconds);
        this.currentMethod.set(method);
        this.currentWindowSeconds.set(Math.max(1, windowSeconds));
        this.windowSecondsSink.tryEmitNext(this.currentWindowSeconds.get());
    }

    public java.util.Map<String, Object> getConfiguration() {
        return java.util.Map.of(
                "method", currentMethod.get(),
                "windowSeconds", currentWindowSeconds.get());
    }
}
