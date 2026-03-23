package com.iot.analytics.service;

import com.iot.analytics.statistics.DeviceStatistics;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;
import com.iot.shared.domain.AnalyticsData;
import com.iot.shared.domain.DeviceData;
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

    private final DeviceStatistics deviceStatistics = new DeviceStatistics(StatsConfig.all());
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
     * Основной метод расчета.
     * Возвращает Mono<AnalyticsData>, который содержит результаты.
     * Consumer должен подписаться на этот метод и отправить результат в RabbitMQ (желательно батчем).
     */
    public Mono<AnalyticsData> calculateStats(List<DeviceData> deviceData) {
        String method = currentMethod.get();
        int batchSize = currentBatchSize.get();

        if (deviceData.isEmpty()) {
            return Mono.empty();
        }

        return computeStats(deviceData, method, batchSize)
                .map(stats -> {
                    // Используем ID первого устройства как идентификатор группы/окна
                    long distinctId = deviceData.getFirst().id();

                    // Создаем объект данных для аналитики
                    return new AnalyticsData(
                            distinctId,
                            Instant.now(),
                            stats.getMetrics());
                })
                .doOnError(error -> log.error("Error calculating stats with method {}", method, error));
    }

    private Mono<DeviceStats> computeStats(List<DeviceData> deviceData, String method, int batchSize) {
        if (deviceData.isEmpty()) {
            return Mono.just(DeviceStats.builder().build());
        }

        return switch (method) {
            case "Sequential" ->
                    Mono.fromCallable(() -> deviceStatistics.computeSequential(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
            case "StandardCollectors" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithStandardCollectors(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
            case "StandardCollectorsParallel" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithStandardCollectorsParallel(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
            case "StandardCollectorsParallelBatch" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithStandardCollectorsParallel(deviceData, batchSize))
                            .subscribeOn(Schedulers.boundedElastic());
            case "CustomCollector" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithCustomCollector(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
            case "CustomCollectorParallel" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithCustomCollectorParallel(deviceData))
                            .subscribeOn(Schedulers.boundedElastic());
            case "CustomCollectorParallelBatch" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithCustomCollectorParallel(deviceData, batchSize))
                            .subscribeOn(Schedulers.boundedElastic());

            // Reactive Methods - conversion logic
            case "Observable" ->
                    Mono.from(deviceStatistics.computeObservable(deviceData, batchSize)
                            .toFlowable(io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER));
            case "Flowable" ->
                    Mono.from(deviceStatistics.computeFlowable(deviceData, batchSize));
            case "FlowableParallel" ->
                    Mono.from(deviceStatistics.computeFlowableParallel(deviceData, batchSize,
                            Runtime.getRuntime().availableProcessors()));
            case "ObservableSync" ->
                    Mono.fromCallable(() -> deviceStatistics.computeObservableSync(deviceData, batchSize))
                            .subscribeOn(Schedulers.boundedElastic());
            case "FlowableSync" ->
                    Mono.fromCallable(() -> deviceStatistics.computeFlowableSync(deviceData, batchSize))
                            .subscribeOn(Schedulers.boundedElastic());
            case "FlowableParallelSync" ->
                    Mono.fromCallable(() -> deviceStatistics.computeFlowableParallelSync(deviceData, batchSize,
                                    Runtime.getRuntime().availableProcessors()))
                            .subscribeOn(Schedulers.boundedElastic());
            case "CustomSubscriber" ->
                    Mono.fromCallable(() -> deviceStatistics.computeWithCustomSubscriber(deviceData, batchSize))
                            .subscribeOn(Schedulers.boundedElastic());

            default ->
                    Mono.fromCallable(() -> deviceStatistics.computeSequential(deviceData))
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