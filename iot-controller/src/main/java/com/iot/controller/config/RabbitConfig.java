package com.iot.controller.config;

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

    @Value("${app.rabbitmq.exchange.analytics}")
    private String analyticsExchangeName;

    @Value("${app.rabbitmq.exchange.alerts}")
    private String alertsExchangeName;

    @Value("${app.rabbitmq.queue.persistence}")
    private String analyticsQueueName;

    @Value("${app.rabbitmq.queue.alerts}")
    private String alertsQueueName;

    // ==================== Exchanges ====================

    @Bean
    public FanoutExchange dataExchange() {
        return new FanoutExchange(dataExchangeName);
    }

    @Bean
    public FanoutExchange analyticsExchange() {
        return new FanoutExchange(analyticsExchangeName);
    }

    @Bean
    public FanoutExchange alertsExchange() {
        return new FanoutExchange(alertsExchangeName);
    }

    // ==================== Queues ====================

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(analyticsQueueName).build();
    }

    @Bean
    public Queue alertsQueue() {
        return QueueBuilder.durable(alertsQueueName).build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, FanoutExchange analyticsExchange) {
        return BindingBuilder.bind(analyticsQueue).to(analyticsExchange);
    }

    @Bean
    public Binding alertsBinding(Queue alertsQueue, FanoutExchange alertsExchange) {
        return BindingBuilder.bind(alertsQueue).to(alertsExchange);
    }

    // ==================== Converters ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
