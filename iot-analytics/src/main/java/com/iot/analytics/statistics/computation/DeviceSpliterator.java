package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.DeviceData;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public final class DeviceSpliterator implements Spliterator<DeviceData> {
    private final List<DeviceData> deviceData;
    private final int endExclusive;
    private int currentIndex;
    private final int batchThreshold;

    public DeviceSpliterator(List<DeviceData> deviceData, int startInclusive, int endExclusive, int batchThreshold) {
        this.deviceData = deviceData;
        this.currentIndex = startInclusive;
        this.endExclusive = endExclusive;
        this.batchThreshold = Math.max(1, batchThreshold);
    }

    public DeviceSpliterator(List<DeviceData> deviceData, int batchThreshold) {
        this(deviceData, 0, deviceData != null ? deviceData.size() : 0, batchThreshold);
    }

    @Override
    public boolean tryAdvance(Consumer<? super DeviceData> action) {
        if (currentIndex >= endExclusive || action == null)
            return false;
        DeviceData d = deviceData.get(currentIndex++);
        action.accept(d);
        return true;
    }

    @Override
    public Spliterator<DeviceData> trySplit() {
        int remaining = endExclusive - currentIndex;
        if (remaining <= batchThreshold) {
            return null;
        }
        int mid = currentIndex + (remaining / 2);
        DeviceSpliterator prefix = new DeviceSpliterator(deviceData, currentIndex, mid, batchThreshold);
        this.currentIndex = mid;
        return prefix;
    }

    @Override
    public long estimateSize() {
        return endExclusive - currentIndex;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL
                | Spliterator.IMMUTABLE;
    }
}
