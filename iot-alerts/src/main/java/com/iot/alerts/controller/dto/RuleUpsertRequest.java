package com.iot.alerts.controller.dto;

import com.iot.alerts.model.RuleType;
import com.iot.alerts.model.Severity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleUpsertRequest(
        @NotBlank String name,
        @NotNull RuleType type,
        @NotNull Severity severity,
        @NotBlank String field,
        @NotBlank String operator,
        Double thresholdNumber,
        Boolean thresholdBoolean,
        String thresholdText,
        @Min(1) int requiredPackets,
        @Min(0) int cooldownSeconds,
        boolean enabled) {
}
