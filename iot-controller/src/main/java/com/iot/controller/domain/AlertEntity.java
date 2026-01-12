package com.iot.controller.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "alerts")
public record AlertEntity(
        @Id String id,
        long deviceId,
        String ruleId,
        String ruleName,
        String severity,
        Object currentValue,
        Object threshold,
        LocalDateTime alertTimestamp,
        String ruleType,
        Instant receivedAt) {
}