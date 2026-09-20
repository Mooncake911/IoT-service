package com.iot.analytics.statistics;

import com.iot.analytics.statistics.model.DeviceStats;
import com.iot.contracts.domain.DeviceData;
import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceStatistics Tests")
class DeviceStatisticsTest {

    private static DeviceData device(boolean online, int battery, int signal) {
        return DeviceData.builder()
                .id(1L)
                .name("sensor")
                .manufacturer("acme")
                .type(Type.SENSOR_TEMPERATURE)
                .location(Location.builder().x(1).y(2).z(0).build())
                .status(Status.builder()
                        .isOnline(online)
                        .batteryLevel(battery)
                        .signalStrength(signal)
                        .lastHeartbeat(Instant.now())
                        .build())
                .build();
    }

    @Test
    @DisplayName("sequential and parallel compute the same metrics")
    void compute_shouldAggregateBothModes() {
        DeviceStatistics statistics = new DeviceStatistics();
        List<DeviceData> batch = List.of(
                device(true, 80, 70),
                device(false, 40, 30));

        DeviceStats sequential = statistics.computeSequential(batch);
        DeviceStats parallel = statistics.computeParallel(batch);

        assertThat(sequential.getMetrics())
                .containsEntry("totalDevices", 2.0)
                .containsEntry("onlineDevices", 1.0)
                .containsEntry("avgBatteryLevel", 60.0)
                .containsEntry("avgSignalStrength", 50.0);
        assertThat(parallel.getMetrics()).isEqualTo(sequential.getMetrics());
    }

    @Test
    @DisplayName("empty batch produces empty metrics")
    void compute_shouldHandleEmpty() {
        DeviceStatistics statistics = new DeviceStatistics();

        assertThat(statistics.computeSequential(List.of()).getMetrics()).isEmpty();
        assertThat(statistics.computeParallel(List.of()).getMetrics()).isEmpty();
    }

    @Test
    @DisplayName("null batch produces empty metrics")
    void compute_shouldHandleNull() {
        DeviceStatistics statistics = new DeviceStatistics();

        assertThat(statistics.computeSequential(null).getMetrics()).isEmpty();
        assertThat(statistics.computeParallel(null).getMetrics()).isEmpty();
    }

    @Test
    @DisplayName("devices without status still count as devices")
    void compute_shouldCountStatusless() {
        DeviceStatistics statistics = new DeviceStatistics();
        DeviceData noStatus = DeviceData.builder().build();

        DeviceStats stats = statistics.computeSequential(List.of(noStatus));

        assertThat(stats.getMetrics()).containsEntry("totalDevices", 1.0);
        assertThat(stats.getMetrics()).containsEntry("onlineDevices", 0.0);
    }
}
