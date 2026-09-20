package com.iot.controller.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.rabbitmq.Sender;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Controller RabbitMQConfig Tests")
class RabbitMQConfigTest {

    private RabbitMQConfig config() {
        RabbitMQConfig config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "dataExchangeName", "iot.data");
        return config;
    }

    @Test
    @DisplayName("infrastructure beans are built from properties")
    void infrastructureBeans_shouldUseProperties() {
        RabbitMQConfig config = config();

        FanoutExchange exchange = config.dataExchange();
        assertThat(exchange.getName()).isEqualTo("iot.data");

        assertThat(config.jsonMessageConverter()).isNotNull();
    }

    @Test
    @DisplayName("reactive connection factory copies RabbitProperties")
    void connectionFactory_shouldCopyProperties() {
        RabbitProperties properties = new RabbitProperties();
        properties.setHost("rabbit");
        properties.setPort(5673);
        properties.setUsername("u");
        properties.setPassword("p");

        ConnectionFactory factory = config().connectionFactory(properties);

        assertThat(factory.getHost()).isEqualTo("rabbit");
        assertThat(factory.getPort()).isEqualTo(5673);
        assertThat(factory.getUsername()).isEqualTo("u");
        assertThat(factory.getPassword()).isEqualTo("p");
    }

    @Test
    @DisplayName("sender is created without connecting")
    void sender_shouldBuildLazily() throws IOException, TimeoutException {
        ConnectionFactory clientFactory = mock(ConnectionFactory.class);
        when(clientFactory.newConnection(anyString())).thenReturn(mock(Connection.class));

        Sender sender = config().sender(clientFactory);

        assertThat(sender).isNotNull();
    }
}
