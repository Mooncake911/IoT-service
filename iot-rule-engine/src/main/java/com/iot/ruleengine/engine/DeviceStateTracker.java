package com.iot.ruleengine.engine;

import com.iot.ruleengine.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks device state across multiple packets for duration-based rules.
 * Thread-safe implementation using ConcurrentHashMap and sliding window.
 */
@Component
public class DeviceStateTracker {

    private static final Logger log = LoggerFactory.getLogger(DeviceStateTracker.class);

    /**
     * Key: deviceId + ruleId
     * Value: Circular buffer of last N evaluation results (true/false)
     */
    private final Map<String, Deque<Boolean>> stateHistory = new ConcurrentHashMap<>();

    /**
     * Tracks which duration rules have already triggered for a device
     * to avoid duplicate alerts for the same sustained condition.
     * Key: deviceId + ruleId
     */
    private final Set<String> triggeredAlerts = ConcurrentHashMap.newKeySet();

    /**
     * Records the evaluation result for a duration rule and checks if it should
     * trigger.
     *
     * @param deviceId     Device ID
     * @param rule         The duration rule being evaluated
     * @param conditionMet Whether the condition was met for this packet
     * @return true if the rule should trigger (N consecutive packets with condition
     *         met)
     */
    public boolean recordAndCheck(long deviceId, Rule rule, boolean conditionMet) {
        String key = buildKey(deviceId, rule.id());
        int requiredPackets = rule.requiredPackets();

        // Get or create the history deque for this device+rule
        Deque<Boolean> history = stateHistory.computeIfAbsent(key, k -> new ArrayDeque<>(requiredPackets));

        synchronized (history) {
            // Add new result to the sliding window
            history.addLast(conditionMet);

            // Keep only the last N results
            while (history.size() > requiredPackets) {
                history.removeFirst();
            }

            // Check if we have N consecutive true values
            if (history.size() < requiredPackets) {
                return false; // Not enough data yet
            }

            boolean allTrue = history.stream().allMatch(Boolean::booleanValue);

            if (allTrue) {
                // Check if we already triggered this alert
                if (triggeredAlerts.contains(key)) {
                    log.debug("Duration rule {} for device {} already triggered, skipping", rule.id(), deviceId);
                    return false;
                }
                // Mark as triggered
                triggeredAlerts.add(key);
                log.info("Duration rule {} triggered for device {} after {} consecutive packets",
                        rule.id(), deviceId, requiredPackets);
                return true;
            } else {
                // Condition broken - reset the triggered state so it can trigger again
                if (triggeredAlerts.remove(key)) {
                    log.debug("Duration rule {} for device {} reset - condition no longer met", rule.id(), deviceId);
                }
                return false;
            }
        }
    }

    /**
     * Clears all state for a specific device (e.g., when device is removed).
     */
    public void clearDevice(long deviceId) {
        stateHistory.keySet().removeIf(key -> key.startsWith(deviceId + ":"));
        triggeredAlerts.removeIf(key -> key.startsWith(deviceId + ":"));
    }

    /**
     * Clears all state (for testing or reset).
     */
    public void clearAll() {
        stateHistory.clear();
        triggeredAlerts.clear();
    }

    private String buildKey(long deviceId, String ruleId) {
        return deviceId + ":" + ruleId;
    }
}
