package com.iot.ruleengine.engine;

import com.iot.ruleengine.model.Rule;
import com.iot.ruleengine.rules.HardcodedRules;
import com.iot.ruleengine.service.AlertPublisher;
import com.iot.shared.domain.AlertData;
import com.iot.shared.domain.DeviceData;
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
     * @param deviceData The device data to evaluate
     * @return List of triggered alerts
     */
    public List<AlertData> processDevice(DeviceData deviceData) {
        if (deviceData == null) {
            log.warn("Received null device, skipping");
            return List.of();
        }

        if (deviceData.status() == null) {
            log.debug("Device {} has no status, skipping rule evaluation", deviceData.id());
            return List.of();
        }

        List<AlertData> triggeredAlerts = new ArrayList<>();

        // Process instant rules
        for (Rule rule : hardcodedRules.getInstantRules()) {
            if (rule.evaluate(deviceData)) {
                AlertData alert = createAlert(deviceData, rule);
                triggeredAlerts.add(alert);
                log.info("Instant rule triggered: {}", alert);
            }
        }

        // Process duration rules
        for (Rule rule : hardcodedRules.getDurationRules()) {
            boolean conditionMet = rule.evaluate(deviceData);
            boolean shouldTrigger = stateTracker.recordAndCheck(deviceData.id(), rule, conditionMet);

            if (shouldTrigger) {
                AlertData alert = createAlert(deviceData, rule);
                triggeredAlerts.add(alert);
                log.info("Duration rule triggered: {}", alert);
            }
        }

        // Publish all triggered alerts
        for (AlertData alert : triggeredAlerts) {
            alertPublisher.publish(alert);
        }

        return triggeredAlerts;
    }

    /**
     * Creates an AlertTriggered event from a device and rule.
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
