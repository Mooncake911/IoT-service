package com.iot.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.contracts.domain.DeviceData;
import com.iot.contracts.domain.components.Location;
import com.iot.contracts.domain.components.Status;
import com.iot.contracts.domain.components.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.Receiver;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AmqpConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private AnalyticsPersistence analyticsPersistence;

    @Mock
    private LiveAnalyticsService liveAnalyticsService;

    @Mock
    private Receiver receiver;

    @Mock
    private AcknowledgableDelivery delivery;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private AmqpConsumer amqpConsumer;

    @BeforeEach
    void setUp() {
        when(analyticsService.getBatchSizeFlux()).thenReturn(Flux.just(1));

        amqpConsumer = new AmqpConsumer(receiver, analyticsService, analyticsPersistence, liveAnalyticsService, objectMapper);
        ReflectionTestUtils.setField(amqpConsumer, "queueName", "analytics.test.queue");
        ReflectionTestUtils.setField(amqpConsumer, "timeoutMs", 100);
        ReflectionTestUtils.setField(amqpConsumer, "concurrency", 1);
    }

    @Test
    @DisplayName("Should process message from reactive receiver")
    public void start_shouldProcessIncomingMessages() throws Exception {
        // Arrange
        DeviceData deviceData = DeviceData.builder()
                .id(999L)
                .name("Rabbit Device")
                .manufacturer("Manufacturer")
                .type(Type.SENSOR_TEMPERATURE)
                .location(new Location(1, 2, 0))
                .status(new Status(true, 80, 70, Instant.now()))
                .build();

        byte[] body = objectMapper.writeValueAsBytes(List.of(deviceData));

        when(delivery.getBody()).thenReturn(body);
        when(receiver.consumeManualAck(eq("analytics.test.queue"), any())).thenReturn(Flux.just(delivery));
        when(analyticsService.calculateStats(any())).thenReturn(Mono.just(mock(com.iot.contracts.domain.AnalyticsData.class)));
        when(analyticsPersistence.save(any())).thenReturn(Mono.empty());

        // Act
        amqpConsumer.start();

        // Assert
        verify(analyticsService, timeout(2000)).calculateStats(any());
        verify(delivery, timeout(2000)).ack();
    }
}
