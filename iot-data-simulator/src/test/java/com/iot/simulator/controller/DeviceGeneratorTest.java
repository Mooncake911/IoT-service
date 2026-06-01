package com.iot.simulator.controller;

import com.iot.contracts.domain.DeviceData;
import com.iot.contracts.domain.components.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceGeneratorTest {

    private DeviceGenerator deviceGenerator;

    @BeforeEach
    void setUp() {
        deviceGenerator = new DeviceGenerator();
    }

    @Test
    @DisplayName("Should generate a list of random devices")
    void randomDevices_shouldReturnCorrectCount() {
        int count = 50;
        List<DeviceData> devices = deviceGenerator.randomDevices(count);

        assertThat(devices).hasSize(count);
        assertThat(devices).allSatisfy(device -> {
            assertThat(device.id()).isPositive();
            assertThat(device.name()).isNotEmpty();
            assertThat(device.manufacturer()).isNotEmpty();
            assertThat(device.type()).isNotNull();
            assertThat(device.location()).isNotNull();
            assertThat(device.status()).isNotNull();
            assertThat(device.capabilities()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should update device status realistically")
    void updateDevice_shouldModifyStatus() {
        DeviceData device = deviceGenerator.randomDevice();
        Status initialStatus = device.status();

        // Run many updates to see changes (statistical)
        DeviceData current = device;
        boolean batteryChanged = false;
        boolean signalChanged = false;
        boolean onlineToggled = false;

        for (int i = 0; i < 1000; i++) {
            current = deviceGenerator.updateDevice(current);
            if (current.status().batteryLevel() != initialStatus.batteryLevel()) {
                batteryChanged = true;
            }
            if (current.status().signalStrength() != initialStatus.signalStrength()) {
                signalChanged = true;
            }
            if (current.status().isOnline() != initialStatus.isOnline()) {
                onlineToggled = true;
            }
        }

        // Toggles and changes are probabilistic, but 1000 iterations should trigger them
        assertThat(batteryChanged || signalChanged || onlineToggled).isTrue();
    }

    @Test
    @DisplayName("Should maintain unique IDs across instances")
    void idCounter_shouldBeSequential() {
        DeviceData d1 = deviceGenerator.randomDevice();
        DeviceData d2 = deviceGenerator.randomDevice();

        assertThat(d2.id()).isEqualTo(d1.id() + 1);
    }
}
