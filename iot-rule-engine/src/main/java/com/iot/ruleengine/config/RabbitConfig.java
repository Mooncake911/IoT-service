package com.iot.ruleengine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfig {

    @Value("${app.rabbitmq.exchange.data}")
    private String dataExchangeName;

    @Value("${app.rabbitmq.exchange.alerts}")
    private String alertsExchangeName;

    @Value("${app.rabbitmq.queue.rule-engine.name}")
    private String ruleEngineQueueName;

    @Value("${app.rabbitmq.queue.rule-engine.ttl}")
    private int ruleEngineQueueTtl;

    // ==================== Exchanges ====================

    @Bean
    public FanoutExchange dataExchange() {
        return new FanoutExchange(dataExchangeName);
    }

    @Bean
    public FanoutExchange alertsExchange() {
        return new FanoutExchange(alertsExchangeName);
    }

    // ==================== Queues ====================

    @Bean
    public Queue ruleEngineQueue() {
        return QueueBuilder.durable(ruleEngineQueueName)
                .ttl(ruleEngineQueueTtl)
                .build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding ruleEngineBinding(Queue ruleEngineQueue, FanoutExchange dataExchange) {
        return BindingBuilder.bind(ruleEngineQueue).to(dataExchange);
    }

    // ==================== Converters ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
