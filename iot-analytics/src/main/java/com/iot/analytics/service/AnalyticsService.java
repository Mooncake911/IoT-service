package com.iot.analytics.service;

import com.iot.analytics.repository.ReactiveDeviceRepository;
import com.iot.analytics.statistics.DeviceStatistics;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;
import com.iot.shared.domain.Device;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Service
public class AnalyticsService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AnalyticsService.class);

    private final ReactiveDeviceRepository deviceRepository;
    private final DeviceStatistics deviceStatistics = new DeviceStatistics(StatsConfig.all());

    public AnalyticsService(ReactiveDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Mono<Void> processData(Flux<Device> devices) {
        return devices.doOnNext(deviceRepository::save).then();
    }

    public Mono<DeviceStats> getCurrentStats(String method, int batchSize) {
        return deviceRepository.findRecentDevices(Duration.ofMinutes(5))
                .collectList()
                .flatMap(devices -> computeStatsReactive(devices, method, batchSize))
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    log.error("Stats computation failed", e);
                    return Mono.just(DeviceStats.builder().build());
                });
    }

    private Mono<DeviceStats> computeStatsReactive(List<Device> devices, String method, int batchSize) {
        if (devices.isEmpty()) {
            return Mono.just(DeviceStats.builder().build());
        }

        return switch (method) {
            case "Sequential" ->
                Mono.fromCallable(() -> deviceStatistics.computeSequential(devices))
                        .subscribeOn(Schedulers.boundedElastic());
            case "StandardCollectors" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithStandardCollectors(devices))
                        .subscribeOn(Schedulers.boundedElastic());
            case "StandardCollectorsParallel" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithStandardCollectorsParallel(devices))
                        .subscribeOn(Schedulers.boundedElastic());
            case "StandardCollectorsParallelBatch" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithStandardCollectorsParallel(devices, batchSize))
                        .subscribeOn(Schedulers.boundedElastic());
            case "CustomCollector" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithCustomCollector(devices))
                        .subscribeOn(Schedulers.boundedElastic());
            case "CustomCollectorParallel" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithCustomCollectorParallel(devices))
                        .subscribeOn(Schedulers.boundedElastic());
            case "CustomCollectorParallelBatch" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithCustomCollectorParallel(devices, batchSize))
                        .subscribeOn(Schedulers.boundedElastic());

            // Reactive Methods - conversion logic
            case "Observable" ->
                Mono.from(
                        deviceStatistics.computeObservable(devices, batchSize).toFlowable(BackpressureStrategy.BUFFER));
            case "Flowable" ->
                Mono.from(deviceStatistics.computeFlowable(toFlowable(devices), batchSize));
            case "FlowableParallel" ->
                Mono.from(deviceStatistics.computeFlowableParallel(toFlowable(devices), batchSize,
                        Runtime.getRuntime().availableProcessors()));
            case "ObservableSync" ->
                Mono.fromCallable(() -> deviceStatistics.computeObservableSync(devices, batchSize))
                        .subscribeOn(Schedulers.boundedElastic());
            case "FlowableSync" ->
                Mono.fromCallable(() -> deviceStatistics.computeFlowableSync(toFlowable(devices), batchSize))
                        .subscribeOn(Schedulers.boundedElastic());
            case "FlowableParallelSync" ->
                Mono.fromCallable(() -> deviceStatistics.computeFlowableParallelSync(toFlowable(devices), batchSize,
                        Runtime.getRuntime().availableProcessors()))
                        .subscribeOn(Schedulers.boundedElastic());
            case "CustomSubscriber" ->
                Mono.fromCallable(() -> deviceStatistics.computeWithCustomSubscriber(toFlowable(devices), batchSize))
                        .subscribeOn(Schedulers.boundedElastic());

            default ->
                Mono.fromCallable(() -> deviceStatistics.computeSequential(devices))
                        .subscribeOn(Schedulers.boundedElastic());
        };
    }

    private Flowable<Device> toFlowable(List<Device> devices) {
        return Flowable.fromIterable(devices);
    }
}
