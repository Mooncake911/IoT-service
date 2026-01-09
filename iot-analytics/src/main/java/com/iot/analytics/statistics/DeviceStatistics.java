package com.iot.analytics.statistics;

import com.iot.shared.domain.Device;
import com.iot.analytics.statistics.computation.*;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
    public DeviceStats computeSequential(List<Device> devices) {
        DeviceStatsAccumulator accumulator = new DeviceStatsAccumulator(statsConfig);
        for (Device device : devices) {
            accumulator.accept(device);
        }
        return accumulator.toDeviceStats();
    }

    // 2. Stream API со стандартными коллекторами с конфигурацией
    public DeviceStats computeWithStandardCollectors(List<Device> devices) {
        return standardStats.compute(devices);
    }

    public DeviceStats computeWithStandardCollectorsParallel(List<Device> devices) {
        return standardStats.computeParallel(devices);
    }

    public DeviceStats computeWithStandardCollectorsParallel(List<Device> devices, int batchSize) {
        DeviceSpliterator spliterator = new DeviceSpliterator(devices, batchSize);
        devices = StreamSupport.stream(spliterator, true).toList();
        return standardStats.computeParallel(devices);
    }

    // 3. Stream API с собственным коллектором
    public DeviceStats computeWithCustomCollector(List<Device> devices) {
        return customStats.compute(devices);
    }

    public DeviceStats computeWithCustomCollectorParallel(List<Device> devices) {
        return customStats.computeParallel(devices);
    }

    public DeviceStats computeWithCustomCollectorParallel(List<Device> devices, int batchSize) {
        DeviceSpliterator spliterator = new DeviceSpliterator(devices, batchSize);
        devices = StreamSupport.stream(spliterator, true).toList();
        return customStats.computeParallel(devices);
    }

    // 3. Реактивные методы
    public Observable<DeviceStats> computeObservable(List<Device> devices, int batchSize) {
        return reactiveStats.computeObservable(devices, batchSize);
    }

    public Flowable<DeviceStats> computeFlowable(Flowable<Device> deviceFlow, int batchSize) {
        return reactiveStats.computeFlowable(deviceFlow, batchSize);
    }

    public Flowable<DeviceStats> computeFlowableParallel(Flowable<Device> deviceFlow, int batchSize, int parallelism) {
        return reactiveStats.computeFlowableParallel(deviceFlow, batchSize, parallelism);
    }

    public DeviceStats computeObservableSync(List<Device> devices, int batchSize) {
        return reactiveStats.computeObservableSync(devices, batchSize);
    }

    public DeviceStats computeFlowableSync(Flowable<Device> deviceFlow, int batchSize) {
        return reactiveStats.computeFlowableSync(deviceFlow, batchSize);
    }

    public DeviceStats computeFlowableParallelSync(Flowable<Device> deviceFlow, int batchSize, int parallelism) {
        return reactiveStats.computeFlowableParallelSync(deviceFlow, batchSize, parallelism);
    }

    public DeviceStats computeWithCustomSubscriber(Flowable<Device> deviceFlow, int batchSize) {
        return reactiveStats.computeWithCustomSubscriber(deviceFlow, batchSize);
    }

    public static Flowable<Device> toFlowable(List<Device> devices) {
        return Flowable.fromIterable(devices);
    }

    public static Flowable<Device> toAsyncFlowable(List<Device> devices) {
        return Flowable.fromIterable(devices)
                .subscribeOn(Schedulers.io());
    }
}