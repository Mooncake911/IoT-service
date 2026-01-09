package com.iot.analytics.service;

import com.iot.analytics.config.RabbitConfig;
import com.iot.shared.domain.Device;
import com.iot.analytics.repository.ReactiveDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class AmqpConsumer {

    private final ReactiveDeviceRepository deviceRepository;
    private static final Logger log = LoggerFactory.getLogger(AmqpConsumer.class);

    public AmqpConsumer(ReactiveDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void consumeMessage(Device device) {
        // Received validated data from Controller via RabbitMQ
        deviceRepository.save(device);
        log.debug("Received device update via AMQP: {}", device.getId());
    }
}
