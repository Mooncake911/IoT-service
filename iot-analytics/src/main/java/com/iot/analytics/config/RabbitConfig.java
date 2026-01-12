package com.iot.analytics.config;

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

    @Value("${app.rabbitmq.queue.analytics}")
    private String analyticsQueueName;

    @Value("${app.rabbitmq.queue.analytics.ttl}")
    private int analyticsQueueTtl;

    // ==================== Exchanges ====================

    @Bean
    public FanoutExchange dataExchange() {
        return new FanoutExchange(dataExchangeName);
    }

    @Bean
    public FanoutExchange analyticsExchange() {
        return new FanoutExchange(analyticsExchangeName);
    }

    // ==================== Queues ====================

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(analyticsQueueName)
                .ttl(analyticsQueueTtl)
                .build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, FanoutExchange dataExchange) {
        return BindingBuilder.bind(analyticsQueue).to(dataExchange);
    }

    // ==================== Converters ====================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
