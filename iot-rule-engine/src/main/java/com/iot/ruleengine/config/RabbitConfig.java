package com.iot.ruleengine.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Rule Engine.
 * - Subscribes to iot.data.exchange (FanoutExchange) via iot.rule-engine.queue
 * - Publishes alerts to alerts.exchange (FanoutExchange)
 */
@Configuration
public class RabbitConfig {

    // Incoming: Device data from IoT Controller
    public static final String DATA_EXCHANGE_NAME = "iot.data.exchange";
    public static final String RULE_ENGINE_QUEUE_NAME = "iot.rule-engine.queue";

    // Outgoing: Alerts to IoT Controller
    public static final String ALERTS_EXCHANGE_NAME = "alerts.exchange";

    // ==================== Incoming Configuration ====================

    @Bean
    public FanoutExchange dataExchange() {
        return new FanoutExchange(DATA_EXCHANGE_NAME);
    }

    @Bean
    public Queue ruleEngineQueue() {
        // TTL Strategy: Messages older than 10 seconds are discarded
        return QueueBuilder.durable(RULE_ENGINE_QUEUE_NAME)
                .ttl(10000) // 10 seconds
                .build();
    }

    @Bean
    public Binding ruleEngineBinding(Queue ruleEngineQueue, FanoutExchange dataExchange) {
        return BindingBuilder.bind(ruleEngineQueue).to(dataExchange);
    }

    // ==================== Outgoing Configuration ====================

    @Bean
    public FanoutExchange alertsExchange() {
        return new FanoutExchange(ALERTS_EXCHANGE_NAME);
    }

    // ==================== Message Converter ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
