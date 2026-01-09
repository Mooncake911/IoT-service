package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.Device;
import com.iot.shared.domain.components.Type;
import com.iot.shared.domain.components.Status;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.utils.DeviceUtils;
import com.iot.analytics.statistics.model.StatsConfig;
import com.iot.analytics.statistics.model.StatType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DeviceStatsStandardCollectors {
    private final StatsConfig statsConfig;

    public DeviceStatsStandardCollectors(StatsConfig statsConfig) {
        this.statsConfig = statsConfig;
    }

    public DeviceStats compute(List<Device> devices) {
        return computeInternal(devices, false);
    }

    public DeviceStats computeParallel(List<Device> devices) {
        return computeInternal(devices, true);
    }

    // Общий внутренний метод для обоих режимов
    private DeviceStats computeInternal(List<Device> devices, boolean parallel) {
        if (devices == null || devices.isEmpty()) {
            return DeviceStats.empty();
        }

        DeviceStats.Builder builder = DeviceStats.builder();

        if (statsConfig.isStatEnabled(StatType.COUNT)) {
            builder.withCount(countTotalDevices(devices, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.ONLINE_COUNT)) {
            builder.withOnlineCount(countOnlineDevices(devices, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.BATTERY)) {
            BatteryStats batteryStats = computeBatteryStats(devices, parallel);
            builder.withBattery(batteryStats.avg, batteryStats.min, batteryStats.max);
        }

        if (statsConfig.isStatEnabled(StatType.SIGNAL)) {
            SignalStats signalStats = computeSignalStats(devices, parallel);
            builder.withSignal(signalStats.avg, signalStats.min, signalStats.max);
        }

        if (statsConfig.isStatEnabled(StatType.HEARTBEAT)) {
            final LocalDateTime now = LocalDateTime.now();
            HeartbeatStats heartbeatStats = computeHeartbeatStats(devices, now, parallel);
            builder.withHeartbeat(heartbeatStats.avg, heartbeatStats.min, heartbeatStats.max);
        }

        if (statsConfig.isStatEnabled(StatType.COVERAGE_VOLUME)) {
            LocationStats locationStats = computeLocationStats(devices, parallel);
            double coverageVolume = DeviceUtils.calculateCoverageVolume(
                    locationStats.minX, locationStats.maxX,
                    locationStats.minY, locationStats.maxY,
                    locationStats.minZ, locationStats.maxZ, devices.size());
            builder.withCoverage(coverageVolume);
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_TYPE)) {
            builder.withDevicesByType(computeDevicesByType(devices, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_MANUFACTURER)) {
            builder.withDevicesByManufacturer(computeDevicesByManufacturer(devices, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_CAPABILITIES)) {
            builder.withDevicesByCapabilities(computeDevicesByCapabilities(devices, parallel));
        }

        return builder.build();
    }

    // Перегруженные вспомогательные методы с поддержкой параллелизма
    private static long countTotalDevices(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        return stream
                .filter(device -> true) // Сбиваем флаг SIZED
                .count();
    }

    private static long countOnlineDevices(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        return stream
                .filter(device -> {
                    Status status = device.getStatus();
                    return status != null && status.isOnline();
                })
                .count();
    }

    private static BatteryStats computeBatteryStats(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        DoubleSummaryStatistics stats = stream
                .map(Device::getStatus)
                .filter(Objects::nonNull)
                .mapToDouble(Status::batteryLevel)
                .summaryStatistics();
        return new BatteryStats(stats.getAverage(), stats.getMin(), stats.getMax());
    }

    private static SignalStats computeSignalStats(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        DoubleSummaryStatistics stats = stream
                .map(Device::getStatus)
                .filter(Objects::nonNull)
                .mapToDouble(Status::signalStrength)
                .summaryStatistics();
        return new SignalStats(stats.getAverage(), stats.getMin(), stats.getMax());
    }

    private static HeartbeatStats computeHeartbeatStats(List<Device> devices, LocalDateTime now, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        LongSummaryStatistics stats = stream
                .map(Device::getStatus)
                .filter(Objects::nonNull)
                .filter(status -> status.lastHeartbeat() != null)
                .mapToLong(status -> Duration.between(status.lastHeartbeat(), now).toMinutes())
                .summaryStatistics();

        double average = stats.getCount() > 0 ? stats.getAverage() : 0;
        long min = stats.getCount() > 0 ? stats.getMin() : 0;
        long max = stats.getCount() > 0 ? stats.getMax() : 0;

        return new HeartbeatStats(average, min, max);
    }

    private static LocationStats computeLocationStats(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();

        // Используем reduce для одного прохода по коллекции
        return stream
                .map(Device::getLocation)
                .filter(Objects::nonNull)
                .reduce(
                        new LocationStats(Integer.MAX_VALUE, Integer.MIN_VALUE,
                                Integer.MAX_VALUE, Integer.MIN_VALUE,
                                Integer.MAX_VALUE, Integer.MIN_VALUE),
                        (acc, loc) -> new LocationStats(
                                Math.min(acc.minX, loc.x()), Math.max(acc.maxX, loc.x()),
                                Math.min(acc.minY, loc.y()), Math.max(acc.maxY, loc.y()),
                                Math.min(acc.minZ, loc.z()), Math.max(acc.maxZ, loc.z())),
                        (acc1, acc2) -> new LocationStats(
                                Math.min(acc1.minX, acc2.minX), Math.max(acc1.maxX, acc2.maxX),
                                Math.min(acc1.minY, acc2.minY), Math.max(acc1.maxY, acc2.maxY),
                                Math.min(acc1.minZ, acc2.minZ), Math.max(acc1.maxZ, acc2.maxZ)));
    }

    private static Map<Type, Long> computeDevicesByType(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        return parallel
                ? stream.collect(Collectors.groupingByConcurrent(Device::getType, Collectors.counting()))
                : stream.collect(Collectors.groupingBy(Device::getType, Collectors.counting()));
    }

    private static Map<String, Long> computeDevicesByManufacturer(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        return parallel
                ? stream.collect(Collectors.groupingByConcurrent(Device::getManufacturer, Collectors.counting()))
                : stream.collect(Collectors.groupingBy(Device::getManufacturer, Collectors.counting()));
    }

    private static Map<String, Long> computeDevicesByCapabilities(List<Device> devices, boolean parallel) {
        var stream = parallel ? devices.parallelStream() : devices.stream();
        return parallel
                ? stream.flatMap(device -> device.getCapabilities().stream())
                        .collect(Collectors.groupingByConcurrent(capability -> capability, Collectors.counting()))
                : stream.flatMap(device -> device.getCapabilities().stream())
                        .collect(Collectors.groupingBy(capability -> capability, Collectors.counting()));
    }

    // Record'ы для временного хранения статистики
    private record BatteryStats(double avg, double min, double max) {
    }

    private record SignalStats(double avg, double min, double max) {
    }

    private record HeartbeatStats(double avg, long min, long max) {
    }

    private record LocationStats(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    }
}
