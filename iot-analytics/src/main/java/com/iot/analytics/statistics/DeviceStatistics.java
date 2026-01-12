package com.iot.analytics.statistics;

import com.iot.shared.domain.DeviceData;
import com.iot.analytics.statistics.computation.*;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

import java.util.List;
import java.util.stream.StreamSupport;

public final class DeviceStatistics {
    private final StatsConfig statsConfig;
    private final DeviceStatsStandardCollectors standardStats;
    private final DeviceStatsCollector customStats;
    private final DeviceStatsReactive reactiveStats;

    public DeviceStatistics(StatsConfig statsConfig) {
        this.statsConfig = statsConfig;
        this.standardStats = new DeviceStatsStandardCollectors(statsConfig);
        this.customStats = new DeviceStatsCollector(statsConfig);
        this.reactiveStats = new DeviceStatsReactive(statsConfig);
    }

    // 1. Итерационный подход
    public DeviceStats computeSequential(List<DeviceData> deviceData) {
        DeviceStatsAccumulator accumulator = new DeviceStatsAccumulator(statsConfig);
        for (DeviceData device : deviceData) {
            accumulator.accept(device);
        }
        return accumulator.toDeviceStats();
    }

    // 2. Stream API со стандартными коллекторами с конфигурацией
    public DeviceStats computeWithStandardCollectors(List<DeviceData> deviceData) {
        return standardStats.compute(deviceData);
    }

    public DeviceStats computeWithStandardCollectorsParallel(List<DeviceData> deviceData) {
        return standardStats.computeParallel(deviceData);
    }

    public DeviceStats computeWithStandardCollectorsParallel(List<DeviceData> deviceData, int batchSize) {
        DeviceSpliterator spliterator = new DeviceSpliterator(deviceData, batchSize);
        deviceData = StreamSupport.stream(spliterator, true).toList();
        return standardStats.computeParallel(deviceData);
    }

    // 3. Stream API с собственным коллектором
    public DeviceStats computeWithCustomCollector(List<DeviceData> deviceData) {
        return customStats.compute(deviceData);
    }

    public DeviceStats computeWithCustomCollectorParallel(List<DeviceData> deviceData) {
        return customStats.computeParallel(deviceData);
    }

    public DeviceStats computeWithCustomCollectorParallel(List<DeviceData> deviceData, int batchSize) {
        DeviceSpliterator spliterator = new DeviceSpliterator(deviceData, batchSize);
        deviceData = StreamSupport.stream(spliterator, true).toList();
        return customStats.computeParallel(deviceData);
    }

    // 3. Реактивные методы
    public Observable<DeviceStats> computeObservable(List<DeviceData> deviceData, int batchSize) {
        return reactiveStats.computeObservable(deviceData, batchSize);
    }

    public Flowable<DeviceStats> computeFlowable(List<DeviceData> deviceData, int batchSize) {
        return reactiveStats.computeFlowable(toFlowable(deviceData), batchSize);
    }

    public Flowable<DeviceStats> computeFlowableParallel(List<DeviceData> deviceData, int batchSize, int parallelism) {
        return reactiveStats.computeFlowableParallel(toFlowable(deviceData), batchSize, parallelism);
    }

    public DeviceStats computeObservableSync(List<DeviceData> deviceData, int batchSize) {
        return reactiveStats.computeObservableSync(deviceData, batchSize);
    }

    public DeviceStats computeFlowableSync(List<DeviceData> deviceData, int batchSize) {
        return reactiveStats.computeFlowableSync(toFlowable(deviceData), batchSize);
    }

    public DeviceStats computeFlowableParallelSync(List<DeviceData> deviceData, int batchSize, int parallelism) {
        return reactiveStats.computeFlowableParallelSync(toFlowable(deviceData), batchSize, parallelism);
    }

    public DeviceStats computeWithCustomSubscriber(List<DeviceData> deviceData, int batchSize) {
        return reactiveStats.computeWithCustomSubscriber(toFlowable(deviceData), batchSize);
    }

    public static Flowable<DeviceData> toFlowable(List<DeviceData> deviceData) {
        return Flowable.fromIterable(deviceData);
    }
}