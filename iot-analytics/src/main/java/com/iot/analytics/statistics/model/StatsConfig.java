package com.iot.analytics.statistics.model;

import java.util.EnumSet;
import java.util.Set;

public final class StatsConfig {
    private final Set<StatType> enabledStats;

    private StatsConfig(Set<StatType> enabledStats) {
        this.enabledStats = enabledStats;
    }

    public boolean isStatEnabled(StatType statType) {
        return enabledStats.contains(statType);
    }

    public static StatsConfig all() {
        return new StatsConfig(EnumSet.allOf(StatType.class));
    }

    public static StatsConfig withStats(StatType... stats) {
        EnumSet<StatType> enabledStats = EnumSet.noneOf(StatType.class);

        for (StatType stat : stats) {
            addStatWithDependencies(stat, enabledStats);
        }

        return new StatsConfig(enabledStats);
    }

    private static void addStatWithDependencies(StatType stat, Set<StatType> enabledStats) {
        // Добавляем саму статистику
        enabledStats.add(stat);

        // Рекурсивно добавляем все зависимые статистики
        for (StatType dependent : stat.getDependentStats()) {
            if (!enabledStats.contains(dependent)) {
                addStatWithDependencies(dependent, enabledStats);
            }
        }
    }

    public Set<StatType> getEnabledStats() {
        return EnumSet.copyOf(enabledStats);
    }

    @Override
    public String toString() {
        return "StatsConfig{enabledStats=" + enabledStats + '}';
    }
}