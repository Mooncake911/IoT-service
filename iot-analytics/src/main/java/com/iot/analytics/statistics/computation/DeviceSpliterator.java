package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.Device;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public final class DeviceSpliterator implements Spliterator<Device> {
    private final List<Device> devices;
    private final int endExclusive;
    private int currentIndex;
    private final int batchThreshold;

    public DeviceSpliterator(List<Device> devices, int startInclusive, int endExclusive, int batchThreshold) {
        this.devices = devices;
        this.currentIndex = startInclusive;
        this.endExclusive = endExclusive;
        this.batchThreshold = Math.max(1, batchThreshold);
    }

    public DeviceSpliterator(List<Device> devices, int batchThreshold) {
        this(devices, 0, devices != null ? devices.size() : 0, batchThreshold);
    }

    @Override
    public boolean tryAdvance(Consumer<? super Device> action) {
        if (currentIndex >= endExclusive || action == null)
            return false;
        Device d = devices.get(currentIndex++);
        action.accept(d);
        return true;
    }

    @Override
    public Spliterator<Device> trySplit() {
        int remaining = endExclusive - currentIndex;
        if (remaining <= batchThreshold) {
            return null;
        }
        int mid = currentIndex + (remaining / 2);
        DeviceSpliterator prefix = new DeviceSpliterator(devices, currentIndex, mid, batchThreshold);
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
