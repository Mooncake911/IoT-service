package com.iot.analytics.repository;

import com.iot.shared.domain.Device;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Repository
public class InMemoryReactiveDeviceRepository implements ReactiveDeviceRepository {

    private final java.util.Map<Long, Device> storage = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Flux<Device> findRecentDevices(Duration duration) {
        return Flux.fromIterable(storage.values());
    }

    @Override
    public void save(Device device) {
        storage.put(device.getId(), device);
    }
}
