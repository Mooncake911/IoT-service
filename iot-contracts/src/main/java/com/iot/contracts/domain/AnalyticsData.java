package com.iot.contracts.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

@Builder(toBuilder = true)
@Jacksonized
public record AnalyticsData(
                long deviceId,
                @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant timestamp,
                @NotEmpty Map<String, Object> metrics) {
}
