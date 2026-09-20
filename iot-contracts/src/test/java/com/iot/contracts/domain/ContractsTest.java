package com.iot.contracts.domain;

import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Contracts Tests")
class ContractsTest {

    @Test
    @DisplayName("DeviceData normalizes null capabilities to empty list")
    void deviceData_shouldNormalizeNullCapabilities() {
        DeviceData device = DeviceData.builder()
                .id(1L)
                .name("sensor")
                .manufacturer("acme")
                .type(Type.SENSOR_TEMPERATURE)
                .location(Location.builder().x(1).y(2).z(0).build())
                .status(Status.builder().isOnline(true).batteryLevel(80).signalStrength(70)
                        .lastHeartbeat(Instant.now()).build())
                .build();

        assertThat(device.capabilities()).isEmpty();
        assertThat(device.toString()).contains("Device{id=1");
    }

    @Test
    @DisplayName("DeviceData copies capabilities defensively")
    void deviceData_shouldCopyCapabilities() {
        DeviceData device = DeviceData.builder()
                .id(2L)
                .name("sensor")
                .manufacturer("acme")
                .type(Type.CAMERA)
                .capabilities(List.of("video", "audio"))
                .location(Location.builder().x(0).y(0).z(0).build())
                .status(Status.builder().isOnline(false).batteryLevel(10).signalStrength(20)
                        .lastHeartbeat(Instant.now()).build())
                .build();

        assertThat(device.capabilities()).containsExactly("video", "audio");
    }

    @Test
    @DisplayName("Status clamps battery and signal to 0..100")
    void status_shouldClampLevels() {
        Status status = new Status(true, -5, 150, Instant.now());

        assertThat(status.batteryLevel()).isEqualTo(0);
        assertThat(status.signalStrength()).isEqualTo(100);
        assertThat(status.toString()).contains("battery=0%");
    }

    @Test
    @DisplayName("Location and Type cover all device kinds")
    void locationAndType_shouldBuild() {
        Location location = Location.builder().x(-10).y(20).z(5).build();

        assertThat(location.toString()).contains("Location[x=-10, y=20, z=5]");
        assertThat(Type.values()).contains(
                Type.SENSOR_TEMPERATURE, Type.SENSOR_HUMIDITY, Type.ACTUATOR_LIGHT,
                Type.ACTUATOR_LOCK, Type.CAMERA, Type.SMART_PLUG, Type.GATEWAY);
    }

    @Test
    @DisplayName("AlertData and AnalyticsData builders hold values")
    void alertAndAnalyticsData_shouldHoldValues() {
        Instant now = Instant.now();
        AlertData alert = AlertData.builder()
                .alertId("a1").deviceId(7L).ruleId("r1").ruleName("low-battery")
                .severity("WARNING").currentValue(3).threshold(20)
                .timestamp(now).ruleType("INSTANT").build();

        assertThat(alert.deviceId()).isEqualTo(7L);
        assertThat(alert.toString()).contains("Device=7");

        AnalyticsData analytics = AnalyticsData.builder()
                .timestamp(now).metrics(Map.of("totalDevices", 2.0)).build();

        assertThat(analytics.metrics()).containsEntry("totalDevices", 2.0);
    }
}
