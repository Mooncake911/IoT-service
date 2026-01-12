package com.iot.shared.domain;

import com.iot.shared.domain.components.Location;
import com.iot.shared.domain.components.Status;
import com.iot.shared.domain.components.Type;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Collections;

public record DeviceData(
        @JsonProperty("id") long id,
        @JsonProperty("name") String name,
        @JsonProperty("manufacturer") String manufacturer,
        @JsonProperty("type") Type type,
        @JsonProperty("capabilities") List<String> capabilities,
        @JsonProperty("location") Location location,
        @JsonProperty("status") Status status) {

    public DeviceData {
        if (capabilities == null) {
            capabilities = Collections.emptyList();
        } else {
            capabilities = List.copyOf(capabilities);
        }
    }

    public DeviceData withLocation(Location newLocation) {
        return new DeviceData(id, name, manufacturer, type, capabilities, newLocation, status);
    }

    public DeviceData withStatus(Status newStatus) {
        return new DeviceData(id, name, manufacturer, type, capabilities, location, newStatus);
    }

    @Override
    public String toString() {
        return String.format(
                "Device{id=%d, name='%s', type=%s, manufacturer='%s', location=%s, status=%s, capabilities=%d}",
                id, name, type, manufacturer, location, status, capabilities != null ? capabilities.size() : 0);
    }
}
