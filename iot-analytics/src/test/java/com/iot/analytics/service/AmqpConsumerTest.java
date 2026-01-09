package com.iot.analytics.service;

import com.iot.analytics.repository.ReactiveDeviceRepository;
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
    private ReactiveDeviceRepository deviceRepository;

    @InjectMocks
    private AmqpConsumer amqpConsumer;

    @Test
    @DisplayName("Should save consumed device to repository")
    public void consumeMessage_shouldSaveDevice() {
        // Arrange
        Device device = new Device();
        device.setId(999L);
        device.setName("Rabbit Device");

        // Act
        amqpConsumer.consumeMessage(device);

        // Assert
        verify(deviceRepository).save(device);
    }
}
