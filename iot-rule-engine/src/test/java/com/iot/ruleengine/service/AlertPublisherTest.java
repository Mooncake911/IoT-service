package com.iot.ruleengine.service;

import com.iot.ruleengine.config.RabbitConfig;
import com.iot.shared.domain.AlertTriggered;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AlertPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AlertPublisher publisher;

    @Test
    @DisplayName("Should send alert to RabbitMQ exchange")
    void publish_shouldSendToRabbit() {
        // Arrange
        AlertTriggered alert = new AlertTriggered(
                "abc", 1L, "RULE", "Rule", "CRITICAL",
                10, 20, LocalDateTime.now(), "INSTANT");

        // Act
        publisher.publish(alert);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.ALERTS_EXCHANGE_NAME), eq(""), eq(alert));
    }
}
