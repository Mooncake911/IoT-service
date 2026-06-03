package com.iot.analytics.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "analytics")
public record AnalyticsEntity(
        @Id String id,
        Instant timestamp,
        Map<String, Object> metrics) {
}

