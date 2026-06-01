package com.iot.contracts.domain;

import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Collections;

@Builder(toBuilder = true)
@Jacksonized
public record DeviceData(
        long id,
        @NotBlank String name,
        @NotBlank String manufacturer,
        @NotNull Type type,
        List<String> capabilities,
        @Valid @NotNull Location location,
        @Valid @NotNull Status status) {

    public DeviceData {
        if (capabilities == null) {
            capabilities = Collections.emptyList();
        } else {
            capabilities = List.copyOf(capabilities);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Device{id=%d, name='%s', type=%s, manufacturer='%s', location=%s, status=%s, capabilities=%d}",
                id, name, type, manufacturer, location, status, capabilities != null ? capabilities.size() : 0);
    }
}
