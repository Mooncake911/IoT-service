package com.iot.alerts.engine;

import com.iot.alerts.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks device state across multiple packets for duration-based rules.
 * Thread-safe implementation with automatic cleanup of stale data.
 */
@Component
public class DeviceStateTracker {

    private static final Logger log = LoggerFactory.getLogger(DeviceStateTracker.class);

    private static final long STATE_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    /**
     * Key: deviceId + ruleId
     * Value: Circular buffer of last N evaluation results
     */
    private final Map<String, Deque<Boolean>> stateHistory = new ConcurrentHashMap<>();

    /**
     * Tracks which duration rules have already triggered
     */
    private final Set<String> triggeredAlerts = ConcurrentHashMap.newKeySet();

    /**
     * Tracks last access time for cleanup
     */
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    private final Map<String, Long> lastTriggeredAt = new ConcurrentHashMap<>();

    public boolean recordAndCheck(long deviceId, Rule rule, boolean conditionMet) {
        String key = buildKey(deviceId, rule.id());
        int requiredPackets = rule.requiredPackets();

        lastAccessTime.put(key, System.currentTimeMillis());

        Deque<Boolean> history = stateHistory.computeIfAbsent(key, k -> new ArrayDeque<>(requiredPackets));

        synchronized (history) {
            history.addLast(conditionMet);

            while (history.size() > requiredPackets) {
                history.removeFirst();
            }

            if (history.size() < requiredPackets) {
                return false;
            }

            boolean allTrue = history.stream().allMatch(Boolean::booleanValue);

            if (allTrue) {
                if (triggeredAlerts.contains(key)) {
                    return false;
                }
                triggeredAlerts.add(key);
                return true;
            } else {
                triggeredAlerts.remove(key);
                return false;
            }
        }
    }

    public boolean canEmitAlert(long deviceId, Rule rule) {
        String key = buildKey(deviceId, rule.id());
        long now = System.currentTimeMillis();
        lastAccessTime.put(key, now);

        int cooldownSeconds = Math.max(0, rule.cooldownSeconds());
        if (cooldownSeconds == 0) {
            lastTriggeredAt.put(key, now);
            return true;
        }

        Long last = lastTriggeredAt.get(key);
        if (last != null && now - last < TimeUnit.SECONDS.toMillis(cooldownSeconds)) {
            return false;
        }

        lastTriggeredAt.put(key, now);
        return true;
    }

    /**
     * Periodic cleanup of stale states to prevent memory leaks.
     */
    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void cleanupStaleStates() {
        long now = System.currentTimeMillis();
        int countBefore = stateHistory.size();

        lastAccessTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > STATE_TTL_MS) {
                String key = entry.getKey();
                stateHistory.remove(key);
                triggeredAlerts.remove(key);
                lastTriggeredAt.remove(key);
                return true;
            }
            return false;
        });

        int countAfter = stateHistory.size();
        if (countBefore != countAfter) {
            log.info("Cleaned up {} stale device states. Active states: {}", countBefore - countAfter, countAfter);
        }
    }

    public void clearDevice(long deviceId) {
        String prefix = deviceId + ":";
        stateHistory.keySet().removeIf(key -> key.startsWith(prefix));
        triggeredAlerts.removeIf(key -> key.startsWith(prefix));
        lastAccessTime.keySet().removeIf(key -> key.startsWith(prefix));
        lastTriggeredAt.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void clearAll() {
        stateHistory.clear();
        triggeredAlerts.clear();
        lastAccessTime.clear();
        lastTriggeredAt.clear();
    }

    private String buildKey(long deviceId, String ruleId) {
        return deviceId + ":" + ruleId;
    }
}
