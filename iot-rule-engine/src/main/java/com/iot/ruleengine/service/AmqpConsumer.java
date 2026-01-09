package com.iot.ruleengine.service;

import com.iot.ruleengine.config.RabbitConfig;
import com.iot.ruleengine.engine.RuleEngine;
import com.iot.shared.domain.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumes Device messages from RabbitMQ and forwards them to RuleEngine for
 * processing.
 * Named AmqpConsumer to match the naming convention in iot-analytics service.
 */
@Service
public class AmqpConsumer {

    private static final Logger log = LoggerFactory.getLogger(AmqpConsumer.class);

    private final RuleEngine ruleEngine;

    public AmqpConsumer(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * Receives Device messages from the iot.rule-engine.queue.
     * Messages are published by IoT Controller to iot.data.exchange
     * (FanoutExchange).
     *
     * @param device The device data from IoT Controller
     */
    @RabbitListener(queues = RabbitConfig.RULE_ENGINE_QUEUE_NAME)
    public void consumeDevice(Device device) {
        log.debug("Received device message: id={}, name={}", device.getId(), device.getName());

        try {
            var alerts = ruleEngine.processDevice(device);
            if (!alerts.isEmpty()) {
                log.info("Device {} triggered {} alert(s)", device.getId(), alerts.size());
            }
        } catch (Exception e) {
            log.error("Error processing device {}: {}", device.getId(), e.getMessage(), e);
        }
    }
}
