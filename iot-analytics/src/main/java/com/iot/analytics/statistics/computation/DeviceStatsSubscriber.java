package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.DeviceData;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceStatsSubscriber implements FlowableSubscriber<DeviceData> {
    private final DeviceStatsAccumulator accumulator;
    private Subscription subscription;
    private final int batchSize;
    private int receivedCount = 0;
    private int requestedCount = 0;
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private volatile Throwable error;

    public DeviceStatsSubscriber(StatsConfig statsConfig, int batchSize) {
        this.accumulator = new DeviceStatsAccumulator(statsConfig);
        this.batchSize = batchSize;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        this.requestedCount = batchSize;
        subscription.request(batchSize);
    }

    @Override
    public void onNext(DeviceData deviceData) {
        if (deviceData != null) {
            accumulator.accept(deviceData);
            receivedCount++;

            if (receivedCount >= requestedCount) {
                requestedCount += batchSize;
                subscription.request(batchSize);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Error in DeviceStatsSubscriber: " + throwable.getMessage());
        error = throwable;
        completed.set(true);
        completionLatch.countDown();
    }

    @Override
    public void onComplete() {
        System.out.println("DeviceStatsSubscriber completed. Processed " + receivedCount + " devices.");
        completed.set(true);
        completionLatch.countDown();
    }

    public DeviceStats getResult() {
        return accumulator.toDeviceStats();
    }

    public CountDownLatch getCompletionLatch() {
        return completionLatch;
    }

    public boolean hasError() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public int getReceivedCount() {
        return receivedCount;
    }

    public void cancel() {
        if (subscription != null) {
            subscription.cancel();
            System.out.println("Subscription cancelled. Processed " + receivedCount + " devices.");
        }
    }
}