package com.iot.shared.domain.components;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Location(
        @JsonProperty("x") int x,
        @JsonProperty("y") int y,
        @JsonProperty("z") int z) {
    @Override
    public String toString() {
        return String.format("Location[x=%d, y=%d, z=%d]", x, y, z);
    }
}
