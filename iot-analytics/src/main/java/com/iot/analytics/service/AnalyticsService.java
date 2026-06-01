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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnalyticsService {

    private final AtomicReference<String> currentMethod;
    private final AtomicInteger currentBatchSize;

    private final DeviceStatistics deviceStatistics = new DeviceStatistics();
    private final Sinks.Many<Integer> batchSizeSink;

    public AnalyticsService(
            @Value("${app.analytics.default-method}") String defaultMethod,
            @Value("${app.analytics.default-batch-size}") int defaultBatchSize) {
        this.currentMethod = new AtomicReference<>(defaultMethod);
        this.currentBatchSize = new AtomicInteger(defaultBatchSize);
        this.batchSizeSink = Sinks.many().replay().latestOrDefault(defaultBatchSize);
    }

    public reactor.core.publisher.Flux<Integer> getBatchSizeFlux() {
        return batchSizeSink.asFlux();
    }

    /**
     * Calculates aggregate metrics for a device batch and returns one analytics record.
     */
    public Mono<AnalyticsData> calculateStats(List<DeviceData> deviceData) {
        String method = currentMethod.get();

        if (deviceData.isEmpty()) {
            return Mono.empty();
        }

        return computeStats(deviceData, method)
                .map(stats -> {
                    // Используем ID первого устройства как идентификатор группы/окна
                    long distinctId = deviceData.getFirst().id();

                    // Создаем объект данных для аналитики
                    return AnalyticsData.builder()
                            .deviceId(distinctId)
                            .timestamp(Instant.now())
                            .metrics(stats.getMetrics())
                            .build();
                })
                .doOnError(error -> log.error("Error calculating stats with method {}", method, error));
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

    public void setCalculationMethod(String method, int batchSize) {
        log.info("Switching analytics calculation method to: {}, batchSize: {}", method, batchSize);
        this.currentMethod.set(method);
        this.currentBatchSize.set(batchSize);
        this.batchSizeSink.tryEmitNext(batchSize);
    }

    public java.util.Map<String, Object> getConfiguration() {
        return java.util.Map.of(
                "method", currentMethod.get(),
                "batchSize", currentBatchSize.get());
    }
}
