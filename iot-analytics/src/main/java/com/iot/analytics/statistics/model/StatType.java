package com.iot.analytics.statistics.model;

import java.util.EnumSet;
import java.util.Set;

public enum StatType {
    COUNT,
    ONLINE_COUNT,
    BATTERY,
    SIGNAL,
    HEARTBEAT,
    STATUS,
    COVERAGE_VOLUME,
    DEVICES_BY_TYPE,
    DEVICES_BY_MANUFACTURER,
    DEVICES_BY_CAPABILITIES;

    private Set<StatType> dependentStats;

    static {
        COUNT.dependentStats = EnumSet.noneOf(StatType.class);
        ONLINE_COUNT.dependentStats = EnumSet.noneOf(StatType.class);
        BATTERY.dependentStats = EnumSet.noneOf(StatType.class);
        SIGNAL.dependentStats = EnumSet.noneOf(StatType.class);
        HEARTBEAT.dependentStats = EnumSet.noneOf(StatType.class);
        STATUS.dependentStats = EnumSet.of(ONLINE_COUNT, BATTERY, SIGNAL, HEARTBEAT);
        COVERAGE_VOLUME.dependentStats = EnumSet.noneOf(StatType.class);
        DEVICES_BY_TYPE.dependentStats = EnumSet.noneOf(StatType.class);
        DEVICES_BY_MANUFACTURER.dependentStats = EnumSet.noneOf(StatType.class);
        DEVICES_BY_CAPABILITIES.dependentStats = EnumSet.noneOf(StatType.class);
    }

    public Set<StatType> getDependentStats() {
        return dependentStats;
    }

    public boolean hasDependencies() {
        return !dependentStats.isEmpty();
    }
}