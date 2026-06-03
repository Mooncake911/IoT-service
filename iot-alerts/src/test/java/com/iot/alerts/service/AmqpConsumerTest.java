package com.iot.alerts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.alerts.engine.RuleEngine;
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
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.Receiver;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AmqpConsumerTest {

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private AlertPersistence alertPersistence;

    @Mock
    private Receiver receiver;

    @Mock
    private AcknowledgableDelivery delivery;

    @Mock
    private RabbitAdmin rabbitAdmin;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private AmqpConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AmqpConsumer(receiver, ruleEngine, alertPersistence, objectMapper, rabbitAdmin);
        ReflectionTestUtils.setField(consumer, "queueName", "alerts.test.queue");
        ReflectionTestUtils.setField(consumer, "batchSize", 10);
        ReflectionTestUtils.setField(consumer, "timeoutMs", 100);
        ReflectionTestUtils.setField(consumer, "concurrency", 1);
    }

    @Test
    @DisplayName("Should forward consumed device from receiver to Rule Engine")
    void start_shouldProcessIncomingMessages() throws Exception {
        // Arrange
        DeviceData deviceData = DeviceData.builder()
                .id(123L)
                .name("Test Device")
                .manufacturer("Acme")
                .type(Type.SENSOR_TEMPERATURE)
                .location(new Location(1, 2, 0))
                .status(new Status(true, 90, 80, Instant.now()))
                .build();
        
        byte[] body = objectMapper.writeValueAsBytes(Collections.singletonList(deviceData));
        
        when(delivery.getBody()).thenReturn(body);
        when(receiver.consumeManualAck(eq("alerts.test.queue"), any())).thenReturn(Flux.just(delivery));
        when(ruleEngine.processDevice(any(DeviceData.class))).thenReturn(List.of());
        // Act
        consumer.start();

        // Assert
        verify(ruleEngine, timeout(2000)).processDevice(any(DeviceData.class));
        verify(delivery, timeout(2000)).ack();
    }
}
