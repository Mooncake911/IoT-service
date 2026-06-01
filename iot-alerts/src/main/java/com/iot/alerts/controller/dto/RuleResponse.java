package com.iot.alerts.controller.dto;

import com.iot.alerts.model.RuleType;
import com.iot.alerts.model.Severity;

import java.time.Instant;

public record RuleResponse(
        String id,
        String name,
        RuleType type,
        Severity severity,
        String field,
        String operator,
        Double thresholdNumber,
        Boolean thresholdBoolean,
        String thresholdText,
        int requiredPackets,
        int cooldownSeconds,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {
}
