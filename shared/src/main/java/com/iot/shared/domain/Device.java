package com.iot.shared.domain;

import com.iot.shared.domain.components.Location;
import com.iot.shared.domain.components.Status;
import com.iot.shared.domain.components.Type;

import java.util.List;

public class Device {

    private long id;
    private String name;
    private String manufacturer;
    private Type type;
    private List<String> capabilities;

    private Location location;
    private Status status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Device() {
    }

    public Device(long id, String name, String manufacturer, Type type, List<String> capabilities,
            Location location, Status status) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.type = type;
        this.capabilities = capabilities;
        this.location = location;
        this.status = status;
    }

    public void updateLocation(Location location) {
        this.location = location;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void addCapability(String capability) {
        if (this.capabilities != null && capability != null && !capability.trim().isEmpty()) {
            this.capabilities.add(capability);
        }
    }

    public void removeCapability(String capability) {
        if (this.capabilities != null) {
            this.capabilities.remove(capability);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Device{id=%d, name='%s', type=%s, manufacturer='%s', location=%s, status=%s, capabilities=%d}",
                id, name, type, manufacturer, location, status, capabilities != null ? capabilities.size() : 0);
    }
}
