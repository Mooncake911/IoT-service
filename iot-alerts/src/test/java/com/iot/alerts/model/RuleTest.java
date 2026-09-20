package com.iot.alerts.model;

import com.iot.contracts.domain.DeviceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rule Tests")
class RuleTest {

    @Test
    @DisplayName("instant factory sets requiredPackets to 1")
    void instant_shouldFixPackets() {
        Rule rule = Rule.instant("id", "name", Severity.WARNING, d -> true, d -> 1, 10, 5);

        assertThat(rule.type()).isEqualTo(RuleType.INSTANT);
        assertThat(rule.requiredPackets()).isEqualTo(1);
        assertThat(rule.cooldownSeconds()).isEqualTo(5);
    }

    @Test
    @DisplayName("duration factory keeps requiredPackets")
    void duration_shouldKeepPackets() {
        Rule rule = Rule.duration("id", "name", Severity.CRITICAL, d -> false, d -> 2, 10, 3, 7);

        assertThat(rule.type()).isEqualTo(RuleType.DURATION);
        assertThat(rule.requiredPackets()).isEqualTo(3);
    }

    @Test
    @DisplayName("evaluate returns false for null device or status")
    void evaluate_shouldGuardNulls() {
        Rule rule = Rule.instant("id", "name", Severity.WARNING, d -> true, d -> 1, 10, 0);

        assertThat(rule.evaluate(null)).isFalse();
        assertThat(rule.evaluate(DeviceData.builder().build())).isFalse();
    }

    @Test
    @DisplayName("extractValue returns null for null device")
    void extractValue_shouldGuardNull() {
        Rule rule = Rule.instant("id", "name", Severity.WARNING, d -> true, d -> 42, 10, 0);

        assertThat(rule.extractValue(null)).isNull();
        assertThat(rule.extractValue(DeviceData.builder().build())).isEqualTo(42);
    }
}
