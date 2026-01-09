package com.iot.ruleengine.engine;

import com.iot.ruleengine.model.Rule;
import com.iot.ruleengine.model.RuleType;
import com.iot.ruleengine.rules.HardcodedRules;
import com.iot.ruleengine.service.AlertPublisher;
import com.iot.shared.domain.AlertTriggered;
import com.iot.shared.domain.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core rule processing engine.
 * Evaluates all rules against incoming device data and generates alerts.
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final HardcodedRules hardcodedRules;
    private final DeviceStateTracker stateTracker;
    private final AlertPublisher alertPublisher;

    public RuleEngine(HardcodedRules hardcodedRules,
            DeviceStateTracker stateTracker,
            AlertPublisher alertPublisher) {
        this.hardcodedRules = hardcodedRules;
        this.stateTracker = stateTracker;
        this.alertPublisher = alertPublisher;
    }

    /**
     * Processes a device message and evaluates all rules.
     * Publishes alerts for any triggered rules.
     *
     * @param device The device data to evaluate
     * @return List of triggered alerts
     */
    public List<AlertTriggered> processDevice(Device device) {
        if (device == null) {
            log.warn("Received null device, skipping");
            return List.of();
        }

        if (device.getStatus() == null) {
            log.debug("Device {} has no status, skipping rule evaluation", device.getId());
            return List.of();
        }

        List<AlertTriggered> triggeredAlerts = new ArrayList<>();

        // Process instant rules
        for (Rule rule : hardcodedRules.getInstantRules()) {
            if (rule.evaluate(device)) {
                AlertTriggered alert = createAlert(device, rule);
                triggeredAlerts.add(alert);
                log.info("Instant rule triggered: {}", alert);
            }
        }

        // Process duration rules
        for (Rule rule : hardcodedRules.getDurationRules()) {
            boolean conditionMet = rule.evaluate(device);
            boolean shouldTrigger = stateTracker.recordAndCheck(device.getId(), rule, conditionMet);

            if (shouldTrigger) {
                AlertTriggered alert = createAlert(device, rule);
                triggeredAlerts.add(alert);
                log.info("Duration rule triggered: {}", alert);
            }
        }

        // Publish all triggered alerts
        for (AlertTriggered alert : triggeredAlerts) {
            alertPublisher.publish(alert);
        }

        return triggeredAlerts;
    }

    /**
     * Creates an AlertTriggered event from a device and rule.
     */
    private AlertTriggered createAlert(Device device, Rule rule) {
        return new AlertTriggered(
                UUID.randomUUID().toString(),
                device.getId(),
                rule.id(),
                rule.name(),
                rule.severity().name(),
                rule.extractValue(device),
                rule.threshold(),
                LocalDateTime.now(),
                rule.type().name());
    }
}
