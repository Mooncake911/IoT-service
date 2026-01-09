package com.iot.analytics.repository;

import com.iot.shared.domain.Device;
import reactor.core.publisher.Flux;

import java.time.Duration;

public interface ReactiveDeviceRepository {
    Flux<Device> findRecentDevices(Duration duration);

    void save(Device device);
}
