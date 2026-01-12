package com.iot.ruleengine.rules;

import com.iot.ruleengine.model.Rule;
import com.iot.ruleengine.model.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HardcodedRules {

        private final int requiredPackets;

        public HardcodedRules(@Value("${rule-engine.duration-rules.required-packets:10}") int requiredPackets) {
                this.requiredPackets = requiredPackets;
        }

        public List<Rule> getInstantRules() {
                return List.of(
                                // Battery rules
                                Rule.instant(
                                                "LOW_BATTERY",
                                                "Низкий заряд батареи",
                                                Severity.WARNING,
                                                device -> device.status() != null
                                                                && device.status().batteryLevel() < 20,
                                                device -> device.status() != null ? device.status().batteryLevel()
                                                                : null,
                                                20),
                                Rule.instant(
                                                "CRITICAL_BATTERY",
                                                "Критический заряд батареи",
                                                Severity.CRITICAL,
                                                device -> device.status() != null
                                                                && device.status().batteryLevel() < 5,
                                                device -> device.status() != null ? device.status().batteryLevel()
                                                                : null,
                                                5),

                                // Signal strength rules
                                Rule.instant(
                                                "LOW_SIGNAL",
                                                "Слабый сигнал",
                                                Severity.WARNING,
                                                device -> device.status() != null
                                                                && device.status().signalStrength() < 30,
                                                device -> device.status() != null
                                                                ? device.status().signalStrength()
                                                                : null,
                                                30),
                                Rule.instant(
                                                "CRITICAL_SIGNAL",
                                                "Критически слабый сигнал",
                                                Severity.CRITICAL,
                                                device -> device.status() != null
                                                                && device.status().signalStrength() < 10,
                                                device -> device.status() != null
                                                                ? device.status().signalStrength()
                                                                : null,
                                                10));
        }

        /**
         * Returns all duration rules.
         * These rules trigger when condition is met for N consecutive packets.
         */
        public List<Rule> getDurationRules() {
                return List.of(
                                // Sustained low battery
                                Rule.duration(
                                                "SUSTAINED_LOW_BATTERY",
                                                "Устойчиво низкий заряд батареи",
                                                Severity.CRITICAL,
                                                device -> device.status() != null
                                                                && device.status().batteryLevel() < 20,
                                                device -> device.status() != null ? device.status().batteryLevel()
                                                                : null,
                                                20,
                                                requiredPackets),

                                // Sustained low signal
                                Rule.duration(
                                                "SUSTAINED_LOW_SIGNAL",
                                                "Устойчиво слабый сигнал",
                                                Severity.CRITICAL,
                                                device -> device.status() != null
                                                                && device.status().signalStrength() < 30,
                                                device -> device.status() != null
                                                                ? device.status().signalStrength()
                                                                : null,
                                                30,
                                                requiredPackets),

                                // Sustained offline
                                Rule.duration(
                                                "SUSTAINED_OFFLINE",
                                                "Долгое отключение устройства",
                                                Severity.CRITICAL,
                                                device -> device.status() != null && !device.status().isOnline(),
                                                device -> device.status() != null ? device.status().isOnline()
                                                                : null,
                                                false,
                                                requiredPackets));
        }

        /**
         * Returns all rules (instant + duration).
         */
        public List<Rule> getAllRules() {
                return java.util.stream.Stream.concat(
                                getInstantRules().stream(),
                                getDurationRules().stream()).toList();
        }
}
