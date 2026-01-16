package com.iot.shared.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NonNull;
import java.time.LocalDateTime;

public record AlertData(
        @JsonProperty("alertId") String alertId,
        @JsonProperty("deviceId") long deviceId,
        @JsonProperty("ruleId") String ruleId,
        @JsonProperty("ruleName") String ruleName,
        @JsonProperty("severity") String severity,
        @JsonProperty("currentValue") Object currentValue,
        @JsonProperty("threshold") Object threshold,
        @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime timestamp,
        @JsonProperty("ruleType") String ruleType) {
    @Override
    @NonNull
    public String toString() {
        return String.format("Alert[%s] Device=%d, Rule=%s (%s), Value=%s, Threshold=%s, Severity=%s",
                alertId, deviceId, ruleId, ruleType, currentValue, threshold, severity);
    }
}
