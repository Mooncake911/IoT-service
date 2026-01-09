package com.iot.controller.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Outgoing: Device data to Analytics and Rule Engine
    public static final String DATA_EXCHANGE_NAME = "iot.data.exchange";

    // Incoming: Alerts from Rule Engine
    public static final String ALERTS_EXCHANGE_NAME = "alerts.exchange";
    public static final String ALERTS_QUEUE_NAME = "alerts.queue";

    // ==================== Outgoing Configuration ====================

    @Bean
    public Exchange iotDataExchange() {
        return new FanoutExchange(DATA_EXCHANGE_NAME);
    }

    // ==================== Incoming Configuration (Alerts) ====================

    @Bean
    public FanoutExchange alertsExchange() {
        return new FanoutExchange(ALERTS_EXCHANGE_NAME);
    }

    @Bean
    public Queue alertsQueue() {
        return QueueBuilder.durable(ALERTS_QUEUE_NAME).build();
    }

    @Bean
    public Binding alertsBinding(Queue alertsQueue, FanoutExchange alertsExchange) {
        return BindingBuilder.bind(alertsQueue).to(alertsExchange);
    }

    // ==================== Message Converter ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
