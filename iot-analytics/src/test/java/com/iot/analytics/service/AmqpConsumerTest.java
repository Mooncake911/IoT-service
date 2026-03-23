package com.iot.analytics.service;

import com.iot.shared.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

@ExtendWith(MockitoExtension.class)
public class AmqpConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private AnalyticsPublisher analyticsPublisher;

    private AmqpConsumer amqpConsumer;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(analyticsService.getBatchSizeFlux()).thenReturn(Flux.just(1));
        amqpConsumer = new AmqpConsumer(analyticsService, analyticsPublisher,100, 2048, 2, 2);
    }

    @Test
    @DisplayName("Should process batch of devices via AnalyticsService")
    public void consumeMessage_shouldProcessBatch() {
        // Arrange
        DeviceData deviceData = new DeviceData(999L, "Rabbit Device", "Manufacturer", null, null, null, null);
        List<DeviceData> batch = List.of(deviceData);
        when(analyticsService.calculateStats(any())).thenReturn(Mono.empty());

        // Act
        amqpConsumer.consumeMessage(batch);

        // Assert
        verify(analyticsService, timeout(2000)).calculateStats(batch);
    }
}
