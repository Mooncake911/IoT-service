package com.iot.alerts.domain;

import com.iot.alerts.model.RuleType;
import com.iot.alerts.model.Severity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "rules")
public record RuleEntity(
        @Id String id,
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
