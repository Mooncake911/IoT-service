package com.iot.ruleengine.service;

import com.iot.ruleengine.engine.RuleEngine;
import com.iot.shared.domain.Device;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AmqpConsumerTest {

    @Mock
    private RuleEngine ruleEngine;

    @InjectMocks
    private AmqpConsumer consumer;

    @Test
    @DisplayName("Should forward consumed device to Rule Engine")
    void consumeDevice_shouldCallRuleEngine() {
        // Arrange
        Device device = new Device();
        device.setId(123L);

        // Act
        consumer.consumeDevice(device);

        // Assert
        verify(ruleEngine).processDevice(device);
    }
}
