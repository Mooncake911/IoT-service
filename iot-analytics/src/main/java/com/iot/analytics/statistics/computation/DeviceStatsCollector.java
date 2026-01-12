package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.DeviceData;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatsConfig;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class DeviceStatsCollector implements Collector<DeviceData, DeviceStatsAccumulator, DeviceStats> {
    private final StatsConfig statsConfig;

    public DeviceStatsCollector(StatsConfig statsConfig) {
        this.statsConfig = statsConfig;
    }

    public DeviceStats compute(List<DeviceData> deviceData) {
        return deviceData.stream()
                .collect(this);
    }

    public DeviceStats computeParallel(List<DeviceData> deviceData) {
        return deviceData.parallelStream()
                .collect(this);
    }

    @Override
    public Supplier<DeviceStatsAccumulator> supplier() {
        return () -> new DeviceStatsAccumulator(statsConfig);
    }

    @Override
    public BiConsumer<DeviceStatsAccumulator, DeviceData> accumulator() {
        return DeviceStatsAccumulator::accept;
    }

    @Override
    public BinaryOperator<DeviceStatsAccumulator> combiner() {
        return DeviceStatsAccumulator::combine;
    }

    @Override
    public Function<DeviceStatsAccumulator, DeviceStats> finisher() {
        return DeviceStatsAccumulator::toDeviceStats;
    }

    @Override
    public Set<Characteristics> characteristics() {
        // Указываем характеристики коллектора:
        // UNORDERED - порядок элементов не важен
        // CONCURRENT - аккумулятор поддерживает параллельную обработку
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
