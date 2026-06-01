package com.iot.alerts.engine;

import com.iot.alerts.model.Rule;
import com.iot.alerts.model.RuleType;
import com.iot.alerts.model.Severity;
import com.iot.alerts.service.RuleManagementService;
import com.iot.contracts.domain.AlertData;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    private RuleEngine ruleEngine;

    @Mock
    private RuleManagementService ruleManagementService;

    @BeforeEach
    void setUp() {
        DeviceStateTracker stateTracker = new DeviceStateTracker();
        ruleEngine = new RuleEngine(ruleManagementService, stateTracker);
    }

    @Test
    @DisplayName("Should trigger instant rule alert")
    void processDevice_InstantRuleTriggered() {
        // Arrange
        Rule instantRule = new Rule(
                "battery-low",
                "Battery Low",
                RuleType.INSTANT,
                Severity.CRITICAL,
                data -> true,
                data -> 5,
                10,
                1,
                0);
        when(ruleManagementService.getInstantRules()).thenReturn(List.of(instantRule));
        when(ruleManagementService.getDurationRules()).thenReturn(List.of());

        DeviceData deviceData = DeviceData.builder()
                .id(1L)
                .name("Test Device")
                .manufacturer("Acme")
                .type(Type.SENSOR_TEMPERATURE)
                .location(new Location(1, 2, 0))
                .status(new Status(true, 5, 80, Instant.now()))
                .build();

        // Act
        List<AlertData> alerts = ruleEngine.processDevice(deviceData);

        // Assert
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).ruleId()).isEqualTo("battery-low");
        assertThat(alerts.get(0).currentValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should trigger duration rule alert after N packets")
    void processDevice_DurationRuleTriggered() {
        // Arrange
        Rule durationRule = new Rule(
                "sustained-temp",
                "High Temp Sustained",
                RuleType.DURATION,
                Severity.WARNING,
                data -> true,
                data -> 50,
                40,
                3,
                0);
        when(ruleManagementService.getInstantRules()).thenReturn(List.of());
        when(ruleManagementService.getDurationRules()).thenReturn(List.of(durationRule));

        DeviceData deviceData = DeviceData.builder()
                .id(1L)
                .name("Test Device")
                .manufacturer("Acme")
                .type(Type.SENSOR_TEMPERATURE)
                .location(new Location(1, 2, 0))
                .status(new Status(true, 100, 80, Instant.now()))
                .build();

        // 1st packet - no trigger
        assertThat(ruleEngine.processDevice(deviceData)).isEmpty();
        // 2nd packet - no trigger
        assertThat(ruleEngine.processDevice(deviceData)).isEmpty();
        // 3rd packet - trigger!
        List<AlertData> alerts = ruleEngine.processDevice(deviceData);
        
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).ruleId()).isEqualTo("sustained-temp");
    }
}
