package com.iot.analytics.statistics;

import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.contracts.domain.DeviceData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;

public class DeviceStatistics {

    public DeviceStats computeSequential(List<DeviceData> deviceData) {
        return compute(deviceData.stream());
    }

    public DeviceStats computeParallel(List<DeviceData> deviceData) {
        return compute(deviceData.parallelStream());
    }

    private DeviceStats compute(java.util.stream.Stream<DeviceData> stream) {
        if (stream == null) {
            return DeviceStats.builder().build();
        }

        StatsAccumulator stats = stream.collect(statsCollector());
        if (stats.totalDevices == 0) {
            return DeviceStats.builder().build();
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalDevices", (double) stats.totalDevices);
        metrics.put("onlineDevices", (double) stats.onlineDevices);
        metrics.put("avgBatteryLevel", stats.totalBattery / (double) stats.totalDevices);
        metrics.put("avgSignalStrength", stats.totalSignal / (double) stats.totalDevices);
        metrics.put("uniqueManufacturers", (double) stats.manufacturers.size());
        metrics.put("uniqueTypes", (double) stats.types.size());

        return DeviceStats.builder().metrics(metrics).build();
    }

    private Collector<DeviceData, StatsAccumulator, StatsAccumulator> statsCollector() {
        return Collector.of(
                StatsAccumulator::new,
                StatsAccumulator::add,
                StatsAccumulator::merge);
    }

    private static final class StatsAccumulator {
        private long totalDevices;
        private long onlineDevices;
        private long totalBattery;
        private long totalSignal;
        private final Set<String> manufacturers = ConcurrentHashMap.newKeySet();
        private final Set<String> types = ConcurrentHashMap.newKeySet();

        private void add(DeviceData deviceData) {
            totalDevices++;
            if (deviceData.status() != null) {
                if (deviceData.status().isOnline()) {
                    onlineDevices++;
                }
                totalBattery += deviceData.status().batteryLevel();
                totalSignal += deviceData.status().signalStrength();
            }

            if (deviceData.manufacturer() != null) {
                manufacturers.add(deviceData.manufacturer());
            }
            if (deviceData.type() != null) {
                types.add(deviceData.type().name());
            }
        }

        private StatsAccumulator merge(StatsAccumulator other) {
            totalDevices += other.totalDevices;
            onlineDevices += other.onlineDevices;
            totalBattery += other.totalBattery;
            totalSignal += other.totalSignal;
            manufacturers.addAll(other.manufacturers);
            types.addAll(other.types);
            return this;
        }
    }
}
