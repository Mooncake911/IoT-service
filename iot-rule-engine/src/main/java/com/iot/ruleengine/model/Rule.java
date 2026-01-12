package com.iot.ruleengine.model;

import com.iot.shared.domain.DeviceData;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a rule that can be evaluated against a Device.
 *
 * @param id              Unique identifier for the rule (e.g., "LOW_BATTERY")
 * @param name            Human-readable name (e.g., "Низкий заряд батареи")
 * @param type            Type of rule (INSTANT or DURATION)
 * @param severity        Alert severity when rule triggers
 * @param condition       Predicate to evaluate against Device
 * @param valueExtractor  Function to extract current value from Device for
 *                        alert
 * @param threshold       Threshold value for the condition
 * @param requiredPackets Number of consecutive packets required (for DURATION
 *                        rules)
 */
public record Rule(
        String id,
        String name,
        RuleType type,
        Severity severity,
        Predicate<DeviceData> condition,
        Function<DeviceData, Object> valueExtractor,
        Object threshold,
        int requiredPackets) {
    /**
     * Creates an INSTANT rule (triggers on single packet).
     */
    public static Rule instant(String id, String name, Severity severity,
            Predicate<DeviceData> condition,
            Function<DeviceData, Object> valueExtractor,
            Object threshold) {
        return new Rule(id, name, RuleType.INSTANT, severity, condition, valueExtractor, threshold, 1);
    }

    /**
     * Creates a DURATION rule (triggers after N consecutive packets).
     */
    public static Rule duration(String id, String name, Severity severity,
            Predicate<DeviceData> condition,
            Function<DeviceData, Object> valueExtractor,
            Object threshold, int requiredPackets) {
        return new Rule(id, name, RuleType.DURATION, severity, condition, valueExtractor, threshold, requiredPackets);
    }

    /**
     * Evaluates the rule condition against a device.
     */
    public boolean evaluate(DeviceData deviceData) {
        if (deviceData == null || deviceData.status() == null) {
            return false;
        }
        return condition.test(deviceData);
    }

    /**
     * Extracts the current value from the device for including in the alert.
     */
    public Object extractValue(DeviceData deviceData) {
        if (deviceData == null) {
            return null;
        }
        return valueExtractor.apply(deviceData);
    }
}
