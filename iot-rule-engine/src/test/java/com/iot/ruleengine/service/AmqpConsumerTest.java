package com.iot.ruleengine.service;

import com.iot.ruleengine.engine.RuleEngine;
import com.iot.shared.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AmqpConsumerTest {

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private AlertPublisher alertPublisher;

    private AmqpConsumer consumer;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        consumer = new AmqpConsumer(ruleEngine, alertPublisher, 500, 500, 1024, 8);
    }

    @Test
    @DisplayName("Should forward consumed device to Rule Engine")
    void consumeDevice_shouldCallRuleEngine() {
        // Arrange
        DeviceData deviceData = new DeviceData(123L, "Test Device", null, null, null, null, null);

        // Act
        consumer.consumeDevice(java.util.List.of(deviceData));

        // Assert
        verify(ruleEngine).processDevice(deviceData);
    }
}
