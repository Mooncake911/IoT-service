package com.iot.alerts.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.data}")
    private String dataExchangeName;

    @Value("${app.rabbitmq.alerts.queue.name}")
    private String alertsQueueName;

    @Value("${app.rabbitmq.alerts.queue.ttl}")
    private int alertsQueueTtl;

    // ==================== Infrastructure (Auto-created by Spring AMQP) ====================

    @Bean
    public FanoutExchange dataExchange() {
        return new FanoutExchange(dataExchangeName);
    }

    @Bean
    public Queue alertsQueue() {
        return QueueBuilder.durable(alertsQueueName)
                .ttl(alertsQueueTtl)
                .build();
    }

    @Bean
    public Binding alertsBinding(Queue alertsQueue, FanoutExchange dataExchange) {
        return BindingBuilder.bind(alertsQueue).to(dataExchange);
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
    public Receiver receiver(ConnectionFactory connectionFactory) {
        Mono<Connection> connectionMono = Mono.fromCallable(() -> connectionFactory.newConnection("alerts-receiver")).cache();
        ReceiverOptions receiverOptions = new ReceiverOptions()
                .connectionMono(connectionMono);
        return RabbitFlux.createReceiver(receiverOptions);
    }

    @Bean
    public Sender sender(ConnectionFactory connectionFactory) {
        Mono<Connection> connectionMono = Mono.fromCallable(() -> connectionFactory.newConnection("alerts-sender")).cache();
        SenderOptions senderOptions = new SenderOptions()
                .connectionMono(connectionMono);
        return RabbitFlux.createSender(senderOptions);
    }
}
