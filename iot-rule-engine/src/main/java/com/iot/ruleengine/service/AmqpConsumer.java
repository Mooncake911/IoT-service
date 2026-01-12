package com.iot.ruleengine.service;

import com.iot.ruleengine.engine.RuleEngine;
import com.iot.shared.domain.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmqpConsumer {

    private final RuleEngine ruleEngine;

    @RabbitListener(queues = "${app.rabbitmq.queue.rule-engine}")
    public void consumeDevice(DeviceData deviceData) {
        log.debug("Received device message via AMQP: id={}, name={}", deviceData.id(), deviceData.name());

        try {
            var alerts = ruleEngine.processDevice(deviceData);
            if (!alerts.isEmpty()) {
                log.info("Device {} triggered {} alert(s)", deviceData.id(), alerts.size());
            }
        } catch (Exception e) {
            log.error("Error processing rules for device {}: {}", deviceData.id(), e.getMessage(), e);
        }
    }
}
