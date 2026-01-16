package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.DeviceData;
import com.iot.shared.domain.components.Type;
import com.iot.shared.domain.components.Status;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.utils.DeviceUtils;
import com.iot.analytics.statistics.model.StatsConfig;
import com.iot.analytics.statistics.model.StatType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class DeviceStatsStandardCollectors {
    private final StatsConfig statsConfig;

    public DeviceStatsStandardCollectors(StatsConfig statsConfig) {
        this.statsConfig = statsConfig;
    }

    public DeviceStats compute(List<DeviceData> deviceData) {
        return computeInternal(deviceData, false);
    }

    public DeviceStats computeParallel(List<DeviceData> deviceData) {
        return computeInternal(deviceData, true);
    }

    // Общий внутренний метод для обоих режимов
    private DeviceStats computeInternal(List<DeviceData> deviceData, boolean parallel) {
        if (deviceData == null || deviceData.isEmpty()) {
            return DeviceStats.empty();
        }

        DeviceStats.Builder builder = DeviceStats.builder();

        if (statsConfig.isStatEnabled(StatType.COUNT)) {
            builder.withCount(countTotalDevices(deviceData, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.ONLINE_COUNT)) {
            builder.withOnlineCount(countOnlineDevices(deviceData, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.BATTERY)) {
            BatteryStats batteryStats = computeBatteryStats(deviceData, parallel);
            builder.withBattery(batteryStats.avg, batteryStats.min, batteryStats.max);
        }

        if (statsConfig.isStatEnabled(StatType.SIGNAL)) {
            SignalStats signalStats = computeSignalStats(deviceData, parallel);
            builder.withSignal(signalStats.avg, signalStats.min, signalStats.max);
        }

        if (statsConfig.isStatEnabled(StatType.HEARTBEAT)) {
            final LocalDateTime now = LocalDateTime.now();
            HeartbeatStats heartbeatStats = computeHeartbeatStats(deviceData, now, parallel);
            builder.withHeartbeat(heartbeatStats.avg, heartbeatStats.min, heartbeatStats.max);
        }

        if (statsConfig.isStatEnabled(StatType.COVERAGE_VOLUME)) {
            LocationStats locationStats = computeLocationStats(deviceData, parallel);
            double coverageVolume = DeviceUtils.calculateCoverageVolume(
                    locationStats.minX, locationStats.maxX,
                    locationStats.minY, locationStats.maxY,
                    locationStats.minZ, locationStats.maxZ, deviceData.size());
            builder.withCoverage(coverageVolume);
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_TYPE)) {
            builder.withDevicesByType(computeDevicesByType(deviceData, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_MANUFACTURER)) {
            builder.withDevicesByManufacturer(computeDevicesByManufacturer(deviceData, parallel));
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_CAPABILITIES)) {
            builder.withDevicesByCapabilities(computeDevicesByCapabilities(deviceData, parallel));
        }

        return builder.build();
    }

    // Перегруженные вспомогательные методы с поддержкой параллелизма
    private static long countTotalDevices(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        return stream
                .filter(device -> true) // Сбиваем флаг SIZED
                .count();
    }

    private static long countOnlineDevices(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        return stream
                .filter(device -> {
                    Status status = device.status();
                    return status != null && status.isOnline();
                })
                .count();
    }

    private static BatteryStats computeBatteryStats(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        DoubleSummaryStatistics stats = stream
                .map(DeviceData::status)
                .filter(Objects::nonNull)
                .mapToDouble(Status::batteryLevel)
                .summaryStatistics();
        return new BatteryStats(stats.getAverage(), stats.getMin(), stats.getMax());
    }

    private static SignalStats computeSignalStats(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        DoubleSummaryStatistics stats = stream
                .map(DeviceData::status)
                .filter(Objects::nonNull)
                .mapToDouble(Status::signalStrength)
                .summaryStatistics();
        return new SignalStats(stats.getAverage(), stats.getMin(), stats.getMax());
    }

    private static HeartbeatStats computeHeartbeatStats(List<DeviceData> deviceData, LocalDateTime now, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        LongSummaryStatistics stats = stream
                .map(DeviceData::status)
                .filter(Objects::nonNull)
                .filter(status -> status.lastHeartbeat() != null)
                .mapToLong(status -> Duration.between(status.lastHeartbeat(), now).toMinutes())
                .summaryStatistics();

        double average = stats.getCount() > 0 ? stats.getAverage() : 0;
        long min = stats.getCount() > 0 ? stats.getMin() : 0;
        long max = stats.getCount() > 0 ? stats.getMax() : 0;

        return new HeartbeatStats(average, min, max);
    }

    private static LocationStats computeLocationStats(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();

        // Используем reduce для одного прохода по коллекции
        return stream
                .map(DeviceData::location)
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

    private static Map<Type, Long> computeDevicesByType(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        return parallel
                ? stream.collect(Collectors.groupingByConcurrent(DeviceData::type, Collectors.counting()))
                : stream.collect(Collectors.groupingBy(DeviceData::type, Collectors.counting()));
    }

    private static Map<String, Long> computeDevicesByManufacturer(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        return parallel
                ? stream.collect(Collectors.groupingByConcurrent(DeviceData::manufacturer, Collectors.counting()))
                : stream.collect(Collectors.groupingBy(DeviceData::manufacturer, Collectors.counting()));
    }

    private static Map<String, Long> computeDevicesByCapabilities(List<DeviceData> deviceData, boolean parallel) {
        var stream = parallel ? deviceData.parallelStream() : deviceData.stream();
        return parallel
                ? stream.flatMap(device -> device.capabilities().stream())
                        .collect(Collectors.groupingByConcurrent(capability -> capability, Collectors.counting()))
                : stream.flatMap(device -> device.capabilities().stream())
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
