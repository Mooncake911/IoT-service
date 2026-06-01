package com.iot.contracts.domain.components;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record Location(
        @Min(-10000) @Max(10000) int x,
        @Min(-10000) @Max(10000) int y,
        int z) {
    @Override
    public String toString() {
        return String.format("Location[x=%d, y=%d, z=%d]", x, y, z);
    }
}
