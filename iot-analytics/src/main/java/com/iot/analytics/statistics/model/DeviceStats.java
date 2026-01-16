package com.iot.analytics.statistics.model;

import com.iot.shared.domain.components.Type;

import lombok.NonNull;
import java.util.HashMap;
import java.util.Map;

public final class DeviceStats {
    private final Long count;
    private final Long onlineCount;

    private final Double avgBattery;
    private final Double minBattery;
    private final Double maxBattery;

    private final Double avgSignal;
    private final Double minSignal;
    private final Double maxSignal;

    private final Double avgHeartbeatDelay;
    private final Double minHeartbeatDelay;
    private final Double maxHeartbeatDelay;

    private final Double coverageVolume;

    private final Map<Type, Long> devicesByType;
    private final Map<String, Long> devicesByManufacturer;
    private final Map<String, Long> devicesByCapabilities;

    private DeviceStats(Builder builder) {
        this.count = builder.count;
        this.onlineCount = builder.onlineCount;
        this.avgBattery = builder.avgBattery;
        this.minBattery = builder.minBattery;
        this.maxBattery = builder.maxBattery;
        this.avgSignal = builder.avgSignal;
        this.minSignal = builder.minSignal;
        this.maxSignal = builder.maxSignal;
        this.avgHeartbeatDelay = builder.avgHeartbeatDelay;
        this.minHeartbeatDelay = builder.minHeartbeatDelay;
        this.maxHeartbeatDelay = builder.maxHeartbeatDelay;
        this.coverageVolume = builder.coverageVolume;
        this.devicesByType = builder.devicesByType;
        this.devicesByManufacturer = builder.devicesByManufacturer;
        this.devicesByCapabilities = builder.devicesByCapabilities;
    }

    public Long getCount() {
        return count;
    }

    public Long getOnlineCount() {
        return onlineCount;
    }

    public Double getAvgBattery() {
        return avgBattery;
    }

    public Double getMinBattery() {
        return minBattery;
    }

    public Double getMaxBattery() {
        return maxBattery;
    }

    public Double getAvgSignal() {
        return avgSignal;
    }

    public Double getMinSignal() {
        return minSignal;
    }

    public Double getMaxSignal() {
        return maxSignal;
    }

    public Double getAvgHeartbeatDelay() {
        return avgHeartbeatDelay;
    }

    public Double getMinHeartbeatDelay() {
        return minHeartbeatDelay;
    }

    public Double getMaxHeartbeatDelay() {
        return maxHeartbeatDelay;
    }

    public Double getCoverageVolume() {
        return coverageVolume;
    }

    public Map<Type, Long> getDevicesByType() {
        return devicesByType;
    }

    public Map<String, Long> getDevicesByManufacturer() {
        return devicesByManufacturer;
    }

    public Map<String, Long> getDevicesByCapabilities() {
        return devicesByCapabilities;
    }

    // Проверки наличия данных
    public boolean hadCountData() {
        return this.count != null;
    }

    public boolean hasOnlineCountData() {
        return onlineCount != null;
    }

    public boolean hasBatteryData() {
        return avgBattery != null;
    }

    public boolean hasSignalData() {
        return avgSignal != null;
    }

    public boolean hasHeartbeatData() {
        return avgHeartbeatDelay != null;
    }

    public boolean hasCoverageData() {
        return coverageVolume != null;
    }

    public boolean hasTypeData() {
        return devicesByType != null;
    }

    public boolean hasManufacturerData() {
        return devicesByManufacturer != null;
    }

    public boolean hasCapabilitiesData() {
        return devicesByCapabilities != null;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder("DeviceStats{");

        if (hadCountData()) {
            sb.append("\n count=").append(count);
        }

        if (hasOnlineCountData()) {
            sb.append("\n online=").append(onlineCount);
        }

        if (hasBatteryData()) {
            sb.append("\n battery[avg=").append(avgBattery).append("%, min=").append(minBattery)
                    .append("%, max=").append(maxBattery).append("%]");
        }
        if (hasSignalData()) {
            sb.append("\n signal[avg=").append(avgSignal).append("%, min=").append(minSignal)
                    .append("%, max=").append(maxSignal).append("%]");
        }
        if (hasHeartbeatData()) {
            sb.append("\n heartbeat[avg=").append(avgHeartbeatDelay).append("min, min=").append(minHeartbeatDelay)
                    .append("min, max=").append(maxHeartbeatDelay).append("min]");
        }
        if (hasCoverageData()) {
            sb.append("\n coverage=").append(coverageVolume);
        }
        if (hasTypeData()) {
            sb.append("\n byType=").append(devicesByType);
        }
        if (hasManufacturerData()) {
            sb.append("\n byManufacturer=").append(devicesByManufacturer);
        }
        if (hasCapabilitiesData()) {
            sb.append("\n byCapabilities=").append(devicesByCapabilities);
        }

        sb.append("\n}");
        return sb.toString();
    }

    // Builder для создания DeviceStats
    public static Builder builder() {
        return new Builder();
    }

    public static DeviceStats empty() {
        return builder().build();
    }

    public static final class Builder {
        private Long count;
        private Long onlineCount;
        private Double avgBattery;
        private Double minBattery;
        private Double maxBattery;
        private Double avgSignal;
        private Double minSignal;
        private Double maxSignal;
        private Double avgHeartbeatDelay;
        private Double minHeartbeatDelay;
        private Double maxHeartbeatDelay;
        private Double coverageVolume;
        private Map<Type, Long> devicesByType;
        private Map<String, Long> devicesByManufacturer;
        private Map<String, Long> devicesByCapabilities;

        // Флаги для отслеживания валидности данных
        private boolean hasBatteryData = false;
        private boolean hasSignalData = false;
        private boolean hasHeartbeatData = false;

        public void withCount(long count) {
            validateNonNegative(count, "count");
            this.count = count;
        }

        public void withOnlineCount(long onlineCount) {
            validateNonNegative(onlineCount, "onlineCount");
            this.onlineCount = onlineCount;
        }

        public void withBattery(double avg, double min, double max) {
            validateBatteryRange(avg, "avgBattery");
            validateBatteryRange(min, "minBattery");
            validateBatteryRange(max, "maxBattery");
            validateMinMaxConsistency(min, max, "battery");

            this.avgBattery = avg;
            this.minBattery = min;
            this.maxBattery = max;
            this.hasBatteryData = true;
        }

        public void withSignal(double avg, double min, double max) {
            validateSignalRange(avg, "avgSignal");
            validateSignalRange(min, "minSignal");
            validateSignalRange(max, "maxSignal");
            validateMinMaxConsistency(min, max, "signal");

            this.avgSignal = avg;
            this.minSignal = min;
            this.maxSignal = max;
            this.hasSignalData = true;
        }

        public void withHeartbeat(double avg, double min, double max) {
            validateNonNegative(avg, "avgHeartbeatDelay");
            validateNonNegative(min, "minHeartbeatDelay");
            validateNonNegative(max, "maxHeartbeatDelay");
            validateMinMaxConsistency(min, max, "heartbeat");

            this.avgHeartbeatDelay = avg;
            this.minHeartbeatDelay = min;
            this.maxHeartbeatDelay = max;
            this.hasHeartbeatData = true;
        }

        public void withCoverage(double volume) {
            validateNonNegative(volume, "coverageVolume");
            this.coverageVolume = volume;
        }

        public void withDevicesByType(Map<Type, Long> devicesByType) {
            validateGroupingMap(devicesByType, "devicesByType");
            this.devicesByType = devicesByType != null ? new HashMap<>(devicesByType) : null;
        }

        public void withDevicesByManufacturer(Map<String, Long> devicesByManufacturer) {
            validateGroupingMap(devicesByManufacturer, "devicesByManufacturer");
            this.devicesByManufacturer = devicesByManufacturer != null ? new HashMap<>(devicesByManufacturer) : null;
        }

        public void withDevicesByCapabilities(Map<String, Long> devicesByCapabilities) {
            validateGroupingMap(devicesByCapabilities, "devicesByCapabilities");
            this.devicesByCapabilities = devicesByCapabilities != null ? new HashMap<>(devicesByCapabilities) : null;
        }

        public DeviceStats build() {
            validateConsistency();
            return new DeviceStats(this);
        }

        // Валидационные методы
        private void validateNonNegative(double value, String fieldName) {
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative: " + value);
            }
        }

        private void validateNonNegative(long value, String fieldName) {
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative: " + value);
            }
        }

        private void validateBatteryRange(double value, String fieldName) {
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException(fieldName + " must be between 0 and 100: " + value);
            }
        }

        private void validateSignalRange(double value, String fieldName) {
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException(fieldName + " must be between 0 and 100: " + value);
            }
        }

        private void validateMinMaxConsistency(double min, double max, String dataType) {
            if (min > max) {
                throw new IllegalArgumentException(
                        dataType + " min value (" + min + ") cannot be greater than max value (" + max + ")");
            }
        }

        private void validateGroupingMap(Map<?, Long> map, String fieldName) {
            if (map != null) {
                for (Map.Entry<?, Long> entry : map.entrySet()) {
                    if (entry.getValue() <= 0) {
                        throw new IllegalArgumentException(fieldName + " contains non-positive count for key "
                                + entry.getKey() + ": " + entry.getValue());
                    }
                }
            }
        }

        private void validateConsistency() {
            // Проверка onlineCount <= count
            if (count != null && onlineCount != null && onlineCount > count) {
                throw new IllegalArgumentException(
                        "onlineCount (" + onlineCount + ") cannot be greater than total count (" + count + ")");
            }

            // Проверка avg между min и max для battery
            if (hasBatteryData && avgBattery != null && minBattery != null && maxBattery != null) {
                if (avgBattery < minBattery || avgBattery > maxBattery) {
                    throw new IllegalArgumentException("avgBattery (" + avgBattery + ") must be between minBattery ("
                            + minBattery + ") and maxBattery (" + maxBattery + ")");
                }
            }

            // Проверка avg между min и max для signal
            if (hasSignalData && avgSignal != null && minSignal != null && maxSignal != null) {
                if (avgSignal < minSignal || avgSignal > maxSignal) {
                    throw new IllegalArgumentException("avgSignal (" + avgSignal + ") must be between minSignal ("
                            + minSignal + ") and maxSignal (" + maxSignal + ")");
                }
            }

            // Проверка avg между min и max для heartbeat
            if (hasHeartbeatData && avgHeartbeatDelay != null && minHeartbeatDelay != null
                    && maxHeartbeatDelay != null) {
                if (avgHeartbeatDelay < minHeartbeatDelay || avgHeartbeatDelay > maxHeartbeatDelay) {
                    throw new IllegalArgumentException(
                            "avgHeartbeatDelay (" + avgHeartbeatDelay + ") must be between minHeartbeatDelay ("
                                    + minHeartbeatDelay + ") and maxHeartbeatDelay (" + maxHeartbeatDelay + ")");
                }
            }
        }
    }
}
