package com.iot.contracts.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Builder(toBuilder = true)
@Jacksonized
public record AlertData(
        @NotBlank String alertId,
        long deviceId,
        @NotBlank String ruleId,
        @NotBlank String ruleName,
        @NotBlank String severity,
        @NotNull Object currentValue,
        @NotNull Object threshold,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant timestamp,
        @NotBlank String ruleType) {

    @Override
    public String toString() {
        return String.format("Alert[%s] Device=%d, Rule=%s (%s), Value=%s, Threshold=%s, Severity=%s",
                alertId, deviceId, ruleId, ruleType, currentValue, threshold, severity);
    }
}
