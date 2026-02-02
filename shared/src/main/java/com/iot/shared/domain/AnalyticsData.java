package com.iot.shared.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NonNull;
import java.time.Instant;
import java.util.Map;

public record AnalyticsData(
                @JsonProperty("deviceId") long deviceId,
                @JsonProperty("timestamp") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
                @JsonProperty("metrics") Map<String, Object> metrics) {
        @Override
        @NonNull
        public String toString() {
                return String.format("Analytics[Device=%d] Time=%s, Metrics=%s",
                                deviceId, timestamp, metrics);
        }
}
