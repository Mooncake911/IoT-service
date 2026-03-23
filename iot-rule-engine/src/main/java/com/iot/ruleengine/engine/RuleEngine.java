package com.iot.ruleengine.engine;

import com.iot.ruleengine.model.Rule;
import com.iot.ruleengine.rules.HardcodedRules;
import com.iot.shared.domain.AlertData;
import com.iot.shared.domain.DeviceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Core rule processing engine.
 * Evaluates all rules against incoming device data and generates alerts.
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final HardcodedRules hardcodedRules;
    private final DeviceStateTracker stateTracker;

    public RuleEngine(HardcodedRules hardcodedRules, DeviceStateTracker stateTracker) {
        this.hardcodedRules = hardcodedRules;
        this.stateTracker = stateTracker;
    }

    /**
     * Processes a device message and evaluates all rules.
     *
     * @param deviceData The device data to evaluate
     * @return List of triggered alerts
     */
    public List<AlertData> processDevice(DeviceData deviceData) {
        if (deviceData == null || deviceData.status() == null) {
            log.debug("Received null device or device with no status, skipping");
            return List.of();
        }

        List<AlertData> triggeredAlerts = new ArrayList<>();

        // Process instant rules
        evaluateRules(hardcodedRules.getInstantRules(), deviceData, (rule, triggered) -> {
            if (triggered) {
                log.info("Instant rule {} triggered for device {}", rule.id(), deviceData.id());
                triggeredAlerts.add(createAlert(deviceData, rule));
            }
        });

        // Process duration rules
        evaluateRules(hardcodedRules.getDurationRules(), deviceData, (rule, conditionMet) -> {
            boolean triggered = stateTracker.recordAndCheck(deviceData.id(), rule, conditionMet);
            if (triggered) {
                log.info("Duration rule {} triggered for device {}", rule.id(), deviceData.id());
                triggeredAlerts.add(createAlert(deviceData, rule));
            } else if (conditionMet) {
                log.debug("Duration rule {} condition met for device {}, but waiting for time window", rule.id(),
                        deviceData.id());
            }
        });

        if (!triggeredAlerts.isEmpty()) {
            log.info("Device {} triggered {} alert(s)", deviceData.id(), triggeredAlerts.size());
        }

        return triggeredAlerts;
    }

    private void evaluateRules(List<Rule> rules, DeviceData data, BiConsumer<Rule, Boolean> resultHandler) {
        for (Rule rule : rules) {
            resultHandler.accept(rule, rule.evaluate(data));
        }
    }

    /**
     * Creates an AlertData from a device and rule.
     */
    private AlertData createAlert(DeviceData deviceData, Rule rule) {
        return new AlertData(
                UUID.randomUUID().toString(),
                deviceData.id(),
                rule.id(),
                rule.name(),
                rule.severity().name(),
                rule.extractValue(deviceData),
                rule.threshold(),
                LocalDateTime.now(),
                rule.type().name());
    }
}
