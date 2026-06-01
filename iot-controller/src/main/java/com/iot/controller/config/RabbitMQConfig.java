package com.iot.controller.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.data}")
    private String dataExchangeName;

    // ==================== Infrastructure (Auto-created by Spring AMQP) ====================

    @Bean
    public FanoutExchange dataExchange() {
        return new FanoutExchange(dataExchangeName);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== Reactive Beans (Reactor RabbitMQ) ====================

    @Bean
    public ConnectionFactory connectionFactory(RabbitProperties properties) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(properties.getHost());
        connectionFactory.setPort(properties.getPort());
        connectionFactory.setUsername(properties.getUsername());
        connectionFactory.setPassword(properties.getPassword());
        return connectionFactory;
    }

    @Bean
    public Sender sender(ConnectionFactory connectionFactory) {
        Mono<Connection> connectionMono = Mono.fromCallable(() -> connectionFactory.newConnection("controller-sender")).cache();
        SenderOptions senderOptions = new SenderOptions()
                .connectionMono(connectionMono);
        return RabbitFlux.createSender(senderOptions);
    }
}
