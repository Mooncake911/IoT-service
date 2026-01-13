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

    @RabbitListener(queues = "${app.rabbitmq.queue.rule-engine.name}")
    public void consumeDevice(java.util.List<DeviceData> deviceDataList) {
        log.debug("Received batch of {} devices via AMQP for rule processing", deviceDataList.size());

        for (DeviceData deviceData : deviceDataList) {
            try {
                var alerts = ruleEngine.processDevice(deviceData);
                if (!alerts.isEmpty()) {
                    log.info("Device {} triggered {} alert(s)", deviceData.id(), alerts.size());
                }
            } catch (Exception e) {
                log.error("Error processing rules for device {}: {}", deviceData.id(), e.getMessage());
            }
        }
    }
}
