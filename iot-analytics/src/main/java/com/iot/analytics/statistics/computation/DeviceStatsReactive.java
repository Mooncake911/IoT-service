package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.Device;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DeviceStatsReactive {
    private final StatsConfig statsConfig;

    public DeviceStatsReactive(StatsConfig statsConfig) {
        this.statsConfig = statsConfig;
    }

    /**
     * Вариант с Observable.
     */
    public Observable<DeviceStats> computeObservable(List<Device> devices, int batchSize) {
        return Observable.fromIterable(devices)
                .buffer(batchSize)
                .flatMap(batch -> Observable.fromCallable(() -> processBatch(batch))
                        .subscribeOn(Schedulers.io()))
                .reduce(new DeviceStatsAccumulator(statsConfig), DeviceStatsAccumulator::combine)
                .map(DeviceStatsAccumulator::toDeviceStats)
                .toObservable();
    }

    /**
     * Вариант с Flowable.
     */
    public Flowable<DeviceStats> computeFlowable(Flowable<Device> deviceFlow, int batchSize) {
        return deviceFlow
                .buffer(batchSize)
                .flatMap(batch -> Flowable.fromCallable(() -> processBatch(batch))
                        .subscribeOn(Schedulers.io()), false, 64)
                .reduce(new DeviceStatsAccumulator(statsConfig), DeviceStatsAccumulator::combine)
                .map(DeviceStatsAccumulator::toDeviceStats)
                .toFlowable();
    }

    /**
     * Вариант с ParallelFlowable (Rails pattern).
     */
    public Flowable<DeviceStats> computeFlowableParallel(Flowable<Device> deviceFlow, int batchSize, int parallelism) {
        return deviceFlow
                .buffer(batchSize)
                .parallel(parallelism)
                .runOn(Schedulers.io())
                .map(this::processBatch)
                .sequential()
                .reduce(new DeviceStatsAccumulator(statsConfig), DeviceStatsAccumulator::combine)
                .map(DeviceStatsAccumulator::toDeviceStats)
                .toFlowable();
    }

    // --- Синхронные методы ---

    public DeviceStats computeObservableSync(List<Device> devices, int batchSize) {
        AtomicReference<DeviceStats> result = new AtomicReference<>(DeviceStats.empty());
        AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            computeObservable(devices, batchSize).blockingSubscribe(result::set, error::set);
        } catch (Exception e) {
            error.set(e);
        }
        if (error.get() != null)
            throw new RuntimeException(error.get());
        return result.get();
    }

    public DeviceStats computeFlowableSync(Flowable<Device> deviceFlow, int batchSize) {
        AtomicReference<DeviceStats> result = new AtomicReference<>(DeviceStats.empty());
        AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            computeFlowable(deviceFlow, batchSize).blockingSubscribe(result::set, error::set);
        } catch (Exception e) {
            error.set(e);
        }
        if (error.get() != null)
            throw new RuntimeException(error.get());
        return result.get();
    }

    public DeviceStats computeFlowableParallelSync(Flowable<Device> deviceFlow, int batchSize, int parallelism) {
        AtomicReference<DeviceStats> result = new AtomicReference<>(DeviceStats.empty());
        AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            computeFlowableParallel(deviceFlow, batchSize, parallelism).blockingSubscribe(result::set, error::set);
        } catch (Exception e) {
            error.set(e);
        }
        if (error.get() != null)
            throw new RuntimeException(error.get());
        return result.get();
    }

    /**
     * Вариант с Custom Subscriber
     */
    public DeviceStats computeWithCustomSubscriber(Flowable<Device> deviceFlow, int batchSize) {
        DeviceStatsSubscriber subscriber = new DeviceStatsSubscriber(statsConfig, batchSize);
        deviceFlow.subscribe(subscriber);

        try {
            subscriber.getCompletionLatch().await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Computation interrupted", e);
        }

        if (subscriber.hasError()) {
            throw new RuntimeException("Error during subscriber processing", subscriber.getError());
        }
        return subscriber.getResult();
    }

    private DeviceStatsAccumulator processBatch(List<Device> batch) {
        DeviceStatsAccumulator accumulator = new DeviceStatsAccumulator(statsConfig);
        for (Device device : batch) {
            if (device != null) {
                accumulator.accept(device);
            }
        }
        return accumulator;
    }
}