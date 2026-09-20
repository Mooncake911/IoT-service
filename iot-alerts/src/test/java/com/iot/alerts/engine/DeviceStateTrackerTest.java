package com.iot.alerts.engine;

import com.iot.alerts.model.Rule;
import com.iot.alerts.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceStateTracker Tests")
class DeviceStateTrackerTest {

    private DeviceStateTracker tracker;

    private static Rule durationRule(String id, int requiredPackets, int cooldownSeconds) {
        return Rule.duration(id, "rule", Severity.WARNING, d -> true, d -> 1, 1, requiredPackets, cooldownSeconds);
    }

    @BeforeEach
    void setUp() {
        tracker = new DeviceStateTracker();
    }

    @Test
    @DisplayName("recordAndCheck triggers after N consecutive true")
    void recordAndCheck_shouldTriggerAfterRequiredPackets() {
        Rule rule = durationRule("r1", 3, 0);

        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isTrue();
    }

    @Test
    @DisplayName("already triggered rule does not fire twice without reset")
    void recordAndCheck_shouldNotRefireWhileTriggered() {
        Rule rule = durationRule("r1", 2, 0);

        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isTrue();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
    }

    @Test
    @DisplayName("false resets the consecutive streak and re-arms trigger")
    void recordAndCheck_shouldResetOnFalse() {
        Rule rule = durationRule("r1", 2, 0);

        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isTrue();
        assertThat(tracker.recordAndCheck(1L, rule, false)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isTrue();
    }

    @Test
    @DisplayName("states are isolated per device")
    void recordAndCheck_shouldIsolateDevices() {
        Rule rule = durationRule("r1", 2, 0);

        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(2L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(1L, rule, true)).isTrue();
        assertThat(tracker.recordAndCheck(2L, rule, true)).isTrue();
    }

    @Test
    @DisplayName("canEmitAlert respects cooldown")
    void canEmitAlert_shouldRespectCooldown() {
        Rule noCooldown = durationRule("r1", 1, 0);
        assertThat(tracker.canEmitAlert(1L, noCooldown)).isTrue();
        assertThat(tracker.canEmitAlert(1L, noCooldown)).isTrue();

        Rule cooled = durationRule("r2", 1, 3600);
        assertThat(tracker.canEmitAlert(7L, cooled)).isTrue();
        assertThat(tracker.canEmitAlert(7L, cooled)).isFalse();
    }

    @Test
    @DisplayName("clearDevice removes only that device state")
    void clearDevice_shouldRemoveOnlyTarget() {
        Rule rule = durationRule("r1", 2, 0);
        tracker.recordAndCheck(1L, rule, true);
        tracker.recordAndCheck(2L, rule, true);

        tracker.clearDevice(1L);

        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
        assertThat(tracker.recordAndCheck(2L, rule, true)).isTrue();
    }

    @Test
    @DisplayName("clearAll resets everything")
    void clearAll_shouldReset() {
        Rule rule = durationRule("r1", 2, 0);
        tracker.recordAndCheck(1L, rule, true);
        tracker.recordAndCheck(1L, rule, true);

        tracker.clearAll();

        assertThat(tracker.recordAndCheck(1L, rule, true)).isFalse();
    }

    @Test
    @DisplayName("cleanupStaleStates keeps fresh states")
    void cleanupStaleStates_shouldKeepFresh() {
        Rule rule = durationRule("r1", 2, 0);
        tracker.recordAndCheck(1L, rule, true);

        tracker.cleanupStaleStates();

        assertThat(tracker.recordAndCheck(1L, rule, true)).isTrue();
    }
}
