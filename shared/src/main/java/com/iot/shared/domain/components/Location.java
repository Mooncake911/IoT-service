package com.iot.shared.domain.components;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NonNull;

public record Location(
        @JsonProperty("x") int x,
        @JsonProperty("y") int y,
        @JsonProperty("z") int z) {
    @Override
    @NonNull
    public String toString() {
        return String.format("Location[x=%d, y=%d, z=%d]", x, y, z);
    }
}
