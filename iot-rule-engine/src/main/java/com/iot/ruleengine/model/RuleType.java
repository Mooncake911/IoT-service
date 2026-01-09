package com.iot.ruleengine.model;

/**
 * Type of rule - determines how the rule is evaluated.
 */
public enum RuleType {
    /**
     * Instant rule - evaluated on each incoming packet independently.
     * Triggers immediately when condition is met.
     */
    INSTANT,

    /**
     * Duration rule - evaluated across multiple packets.
     * Triggers when condition is met for N consecutive packets.
     */
    DURATION
}
