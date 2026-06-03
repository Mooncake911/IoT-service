package com.iot.analytics.service;

import com.iot.contracts.domain.DeviceData;
import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LiveAnalyticsServiceTest {

    @Test
    @DisplayName("Should keep one live snapshot per device and aggregate unique devices")
    void ingestDevices_shouldAggregateUniqueDevices() {
        LiveAnalyticsService service = new LiveAnalyticsService();

        service.ingestDevices(List.of(
                device(1L, "Acme", Type.SENSOR_TEMPERATURE, false, 10, 15),
                device(2L, "Omni", Type.CAMERA, true, 80, 70),
                device(1L, "Acme", Type.SENSOR_TEMPERATURE, true, 90, 95)
        ));

        Map<String, Object> summary = service.getSummary();
        Map<String, Object> byType = service.getByType();
        Map<String, Object> byManufacturer = service.getByManufacturer();

        assertThat(summary.get("totalUniqueDevices")).isEqualTo(2L);
        assertThat(summary.get("onlineCount")).isEqualTo(2L);
        assertThat(summary.get("offlineCount")).isEqualTo(0L);
        assertThat(summary.get("lowBatteryCount")).isEqualTo(0L);
        assertThat(summary.get("weakSignalCount")).isEqualTo(0L);
        assertThat(summary.get("flappingCount")).isEqualTo(0L);
        assertThat(summary.get("avgBatteryLevel")).isEqualTo(85.0);
        assertThat(summary.get("avgSignalStrength")).isEqualTo(82.5);
        assertThat(byType.get("types")).isEqualTo(Map.of(
                Type.SENSOR_TEMPERATURE.name(), 1L,
                Type.CAMERA.name(), 1L
        ));
        assertThat(byManufacturer.get("manufacturers")).isEqualTo(Map.of(
                "Acme", 1L,
                "Omni", 1L
        ));
    }

    @Test
    @DisplayName("Should update live metrics when the same device changes state")
    void ingestDevices_shouldReplaceExistingDeviceSnapshot() {
        LiveAnalyticsService service = new LiveAnalyticsService();

        service.ingestDevices(List.of(device(1L, "Acme", Type.SENSOR_TEMPERATURE, false, 10, 15)));
        service.ingestDevices(List.of(device(1L, "Acme", Type.SENSOR_TEMPERATURE, true, 50, 60)));

        Map<String, Object> summary = service.getSummary();

        assertThat(summary.get("totalUniqueDevices")).isEqualTo(1L);
        assertThat(summary.get("onlineCount")).isEqualTo(1L);
        assertThat(summary.get("offlineCount")).isEqualTo(0L);
        assertThat(summary.get("flappingCount")).isEqualTo(1L);
        assertThat(summary.get("avgBatteryLevel")).isEqualTo(50.0);
        assertThat(summary.get("avgSignalStrength")).isEqualTo(60.0);
    }

    private static DeviceData device(long id, String manufacturer, Type type, boolean online, int battery, int signal) {
        return DeviceData.builder()
                .id(id)
                .name("Device-" + id)
                .manufacturer(manufacturer)
                .type(type)
                .location(new Location(1, 2, 0))
                .status(new Status(online, battery, signal, Instant.now()))
                .build();
    }
}
