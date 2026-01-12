package com.iot.analytics.service;

import com.iot.shared.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AmqpConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AmqpConsumer amqpConsumer;

    @Test
    @DisplayName("Should process batch of devices via AnalyticsService")
    public void consumeMessage_shouldProcessBatch() {
        // Arrange
        DeviceData deviceData = new DeviceData(999L, "Rabbit Device", "Manufacturer", null, null, null, null);
        List<DeviceData> batch = List.of(deviceData);

        // Act
        amqpConsumer.consumeMessage(batch);

        // Assert
        verify(analyticsService).calculateAndPublishStats(batch);
    }
}
