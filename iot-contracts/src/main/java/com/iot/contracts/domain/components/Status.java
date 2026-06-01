package com.iot.contracts.domain.components;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record Status(
        boolean isOnline,
        @Min(0) @Max(100) int batteryLevel,
        @Min(0) @Max(100) int signalStrength,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant lastHeartbeat) {
    
    public Status(boolean isOnline, int batteryLevel, int signalStrength, Instant lastHeartbeat) {
        this.isOnline = isOnline;
        this.batteryLevel = Math.clamp(batteryLevel, 0, 100);
        this.signalStrength = Math.clamp(signalStrength, 0, 100);
        this.lastHeartbeat = lastHeartbeat;
    }

    @Override
    public String toString() {
        return String.format("Status[online=%s, battery=%d%%, signal=%d%%, lastSeen=%s]",
                isOnline, batteryLevel, signalStrength, lastHeartbeat);
    }
}
