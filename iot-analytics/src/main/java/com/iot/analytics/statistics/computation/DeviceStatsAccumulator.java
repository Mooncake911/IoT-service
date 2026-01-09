package com.iot.analytics.statistics.computation;

import com.iot.shared.domain.Device;
import com.iot.shared.domain.components.Location;
import com.iot.shared.domain.components.Status;
import com.iot.shared.domain.components.Type;
import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.analytics.statistics.model.StatType;
import com.iot.analytics.statistics.model.StatsConfig;
import com.iot.analytics.statistics.utils.DeviceUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceStatsAccumulator {
    private final StatsConfig statsConfig;
    private long count = 0;
    private long onlineCount = 0;

    private double sumBattery = 0, minBattery = Double.MAX_VALUE, maxBattery = Double.MIN_VALUE;
    private double sumSignal = 0, minSignal = Double.MAX_VALUE, maxSignal = Double.MIN_VALUE;
    private long sumHeartbeatMinutes = 0, minHeartbeatMinutes = Long.MAX_VALUE, maxHeartbeatMinutes = Long.MIN_VALUE;

    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

    private final Map<Type, Long> devicesByType = new HashMap<>();
    private final Map<String, Long> devicesByManufacturer = new HashMap<>();
    private final Map<String, Long> devicesByCapabilities = new HashMap<>();

    private int validStatusCount = 0;
    private final LocalDateTime nowTime = LocalDateTime.now();

    public DeviceStatsAccumulator(StatsConfig statsConfig) {
        this.statsConfig = statsConfig;
    }

    public void accept(Device device) {
        if (device == null)
            return;

        count++;

        if (statsConfig.isStatEnabled(StatType.ONLINE_COUNT) ||
                statsConfig.isStatEnabled(StatType.BATTERY) ||
                statsConfig.isStatEnabled(StatType.SIGNAL) ||
                statsConfig.isStatEnabled(StatType.HEARTBEAT)) {
            acceptStatus(device.getStatus());
        }

        if (statsConfig.isStatEnabled(StatType.COVERAGE_VOLUME)) {
            acceptLocation(device.getLocation());
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_TYPE)) {
            acceptType(device.getType());
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_MANUFACTURER)) {
            acceptManufacturer(device.getManufacturer());
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_CAPABILITIES)) {
            acceptCapabilities(device.getCapabilities());
        }
    }

    private void acceptStatus(Status status) {
        if (status == null)
            return;

        validStatusCount++;

        if (statsConfig.isStatEnabled(StatType.ONLINE_COUNT)) {
            if (status.isOnline()) {
                onlineCount++;
            }
        }

        if (statsConfig.isStatEnabled(StatType.BATTERY)) {
            double battery = status.batteryLevel();
            sumBattery += battery;
            minBattery = Math.min(minBattery, battery);
            maxBattery = Math.max(maxBattery, battery);
        }

        if (statsConfig.isStatEnabled(StatType.SIGNAL)) {
            double signal = status.signalStrength();
            sumSignal += signal;
            minSignal = Math.min(minSignal, signal);
            maxSignal = Math.max(maxSignal, signal);
        }

        if (statsConfig.isStatEnabled(StatType.HEARTBEAT)) {
            LocalDateTime last = status.lastHeartbeat();
            long minutesAgo = Duration.between(last, nowTime).toMinutes();
            sumHeartbeatMinutes += minutesAgo;
            minHeartbeatMinutes = Math.min(minHeartbeatMinutes, minutesAgo);
            maxHeartbeatMinutes = Math.max(maxHeartbeatMinutes, minutesAgo);
        }
    }

    private void acceptLocation(Location location) {
        if (location == null)
            return;

        minX = Math.min(minX, location.x());
        maxX = Math.max(maxX, location.x());
        minY = Math.min(minY, location.y());
        maxY = Math.max(maxY, location.y());
        minZ = Math.min(minZ, location.z());
        maxZ = Math.max(maxZ, location.z());
    }

    private void acceptType(Type type) {
        devicesByType.merge(type, 1L, Long::sum);
    }

    private void acceptManufacturer(String manufacturer) {
        devicesByManufacturer.merge(manufacturer, 1L, Long::sum);
    }

    private void acceptCapabilities(List<String> capabilities) {
        for (String capability : capabilities) {
            devicesByCapabilities.merge(capability, 1L, Long::sum);
        }
    }

    // Метод для объединения двух аккумуляторов (для параллельных потоков)
    public DeviceStatsAccumulator combine(DeviceStatsAccumulator other) {
        DeviceStatsAccumulator combined = new DeviceStatsAccumulator(this.statsConfig);

        // Объединяем базовые счетчики
        combined.count = this.count + other.count;
        combined.onlineCount = this.onlineCount + other.onlineCount;
        combined.validStatusCount = this.validStatusCount + other.validStatusCount;

        // Объединяем суммы
        combined.sumBattery = this.sumBattery + other.sumBattery;
        combined.sumSignal = this.sumSignal + other.sumSignal;
        combined.sumHeartbeatMinutes = this.sumHeartbeatMinutes + other.sumHeartbeatMinutes;

        // Объединяем минимумы и максимумы
        combined.minBattery = Math.min(this.minBattery, other.minBattery);
        combined.maxBattery = Math.max(this.maxBattery, other.maxBattery);
        combined.minSignal = Math.min(this.minSignal, other.minSignal);
        combined.maxSignal = Math.max(this.maxSignal, other.maxSignal);
        combined.minHeartbeatMinutes = Math.min(this.minHeartbeatMinutes, other.minHeartbeatMinutes);
        combined.maxHeartbeatMinutes = Math.max(this.maxHeartbeatMinutes, other.maxHeartbeatMinutes);

        // Объединяем координаты
        combined.minX = Math.min(this.minX, other.minX);
        combined.maxX = Math.max(this.maxX, other.maxX);
        combined.minY = Math.min(this.minY, other.minY);
        combined.maxY = Math.max(this.maxY, other.maxY);
        combined.minZ = Math.min(this.minZ, other.minZ);
        combined.maxZ = Math.max(this.maxZ, other.maxZ);

        // Объединяем мапы
        combined.devicesByType.putAll(this.devicesByType);
        other.devicesByType.forEach((k, v) -> combined.devicesByType.merge(k, v, Long::sum));

        combined.devicesByManufacturer.putAll(this.devicesByManufacturer);
        other.devicesByManufacturer.forEach((k, v) -> combined.devicesByManufacturer.merge(k, v, Long::sum));

        combined.devicesByCapabilities.putAll(this.devicesByCapabilities);
        other.devicesByCapabilities.forEach((k, v) -> combined.devicesByCapabilities.merge(k, v, Long::sum));

        return combined;
    }

    // Преобразование аккумулятора в конечный результат
    public DeviceStats toDeviceStats() {
        if (count == 0) {
            return DeviceStats.empty();
        }

        DeviceStats.Builder builder = DeviceStats.builder();

        if (statsConfig.isStatEnabled(StatType.COUNT)) {
            builder.withCount(count);
        }

        if (statsConfig.isStatEnabled(StatType.ONLINE_COUNT)) {
            builder.withOnlineCount(onlineCount);
        }

        if (statsConfig.isStatEnabled(StatType.BATTERY) && validStatusCount > 0) {
            double avgBattery = sumBattery / validStatusCount;
            builder.withBattery(avgBattery, minBattery, maxBattery);
        }

        if (statsConfig.isStatEnabled(StatType.SIGNAL) && validStatusCount > 0) {
            double avgSignal = sumSignal / validStatusCount;
            builder.withSignal(avgSignal, minSignal, maxSignal);
        }

        if (statsConfig.isStatEnabled(StatType.HEARTBEAT) && validStatusCount > 0) {
            double avgHeartbeatDelay = (double) sumHeartbeatMinutes / validStatusCount;
            builder.withHeartbeat(avgHeartbeatDelay, minHeartbeatMinutes, maxHeartbeatMinutes);
        }

        if (statsConfig.isStatEnabled(StatType.COVERAGE_VOLUME)) {
            double coverageVolume = DeviceUtils.calculateCoverageVolume(
                    minX, maxX, minY, maxY, minZ, maxZ, count);
            builder.withCoverage(coverageVolume);
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_TYPE)) {
            builder.withDevicesByType(devicesByType);
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_MANUFACTURER)) {
            builder.withDevicesByManufacturer(devicesByManufacturer);
        }

        if (statsConfig.isStatEnabled(StatType.DEVICES_BY_CAPABILITIES)) {
            builder.withDevicesByCapabilities(devicesByCapabilities);
        }

        return builder.build();
    }
}
