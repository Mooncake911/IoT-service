package com.iot.analytics.service;

import com.iot.shared.domain.AnalyticsData;
import com.iot.shared.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import reactor.test.StepVerifier;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsPublisher analyticsPublisher;

    private AnalyticsService analyticsService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsPublisher, "Sequential", 10);
    }

    @Test
    @DisplayName("Should calculate and publish stats when device data is provided")
    void calculateAndPublishStats_Success() {
        // Arrange
        DeviceData deviceData = new DeviceData(1L, "TestDevice", "TestType", null, null, null, null);
        List<DeviceData> batch = List.of(deviceData);

        // Act & Assert
        StepVerifier.create(analyticsService.calculateAndPublishStats(batch))
                .verifyComplete();

        verify(analyticsPublisher, times(1)).publish(any(AnalyticsData.class));
    }

    @Test
    @DisplayName("Should not publish anything when device data is empty")
    void calculateAndPublishStats_EmptyList() {
        // Arrange
        List<DeviceData> batch = Collections.emptyList();

        // Act & Assert
        StepVerifier.create(analyticsService.calculateAndPublishStats(batch))
                .verifyComplete();

        verify(analyticsPublisher, times(0)).publish(any());
    }
}
