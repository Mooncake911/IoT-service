package com.iot.controller.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * MongoDB entity for persisting alerts from Rule Engine.
 */
@Document(collection = "alerts")
public class AlertEntity {

    @Id
    private String id;

    private String alertId;
    private long deviceId;
    private String ruleId;
    private String ruleName;
    private String severity;
    private Object currentValue;
    private Object threshold;
    private LocalDateTime alertTimestamp;
    private String ruleType;
    private Instant receivedAt;

    public AlertEntity() {
    }

    public AlertEntity(String alertId, long deviceId, String ruleId, String ruleName,
            String severity, Object currentValue, Object threshold,
            LocalDateTime alertTimestamp, String ruleType, Instant receivedAt) {
        this.alertId = alertId;
        this.deviceId = deviceId;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.currentValue = currentValue;
        this.threshold = threshold;
        this.alertTimestamp = alertTimestamp;
        this.ruleType = ruleType;
        this.receivedAt = receivedAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Object getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Object currentValue) {
        this.currentValue = currentValue;
    }

    public Object getThreshold() {
        return threshold;
    }

    public void setThreshold(Object threshold) {
        this.threshold = threshold;
    }

    public LocalDateTime getAlertTimestamp() {
        return alertTimestamp;
    }

    public void setAlertTimestamp(LocalDateTime alertTimestamp) {
        this.alertTimestamp = alertTimestamp;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    @Override
    public String toString() {
        return String.format("AlertEntity[%s] Device=%d, Rule=%s, Severity=%s, Value=%s",
                alertId, deviceId, ruleId, severity, currentValue);
    }
}
