package com.iot.analytics.service;

import com.iot.shared.domain.DeviceData;
import com.iot.shared.domain.components.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsServiceTest {

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService("Sequential", 10);
    }

    @Test
    @DisplayName("Should return calculated stats when device data is provided")
    void calculateStats_Success() {
        // Arrange
        DeviceData deviceData = new DeviceData(1L, "TestDevice", "Manufacturer", Type.SENSOR_TEMPERATURE, null, null,
                null);
        List<DeviceData> batch = List.of(deviceData);

        // Act & Assert
        StepVerifier.create(analyticsService.calculateStats(batch))
                .assertNext(data -> {
                    // Проверяем, что данные рассчитаны верно
                    assertThat(data).isNotNull();
                    assertThat(data.deviceId()).isEqualTo(1L);
                    assertThat(data.metrics()).containsKey("totalDevices");
                    assertThat(data.metrics().get("totalDevices")).isEqualTo(1.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when device data is empty")
    void calculateStats_EmptyList() {
        // Arrange
        List<DeviceData> batch = Collections.emptyList();

        // Act & Assert
        StepVerifier.create(analyticsService.calculateStats(batch))
                .verifyComplete();
    }
}