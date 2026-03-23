package com.iot.ruleengine.service;

import com.iot.shared.domain.AlertData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AlertPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AlertPublisher publisher;

    @BeforeEach
    public void setUp() {
        publisher = new AlertPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "alertsExchangeName", "alerts.exchange");
        ReflectionTestUtils.setField(publisher, "publishChunkSize", 100);
    }

    @Test
    @DisplayName("Should send alert to RabbitMQ exchange")
    void publish_shouldSendToRabbit() {
        // Arrange
        AlertData alert = new AlertData(
                "abc", 1L, "RULE", "Rule", "CRITICAL",
                10, 20, LocalDateTime.now(), "INSTANT");

        // Act
        publisher.publish(alert);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("alerts.exchange"), eq(""), eq(alert));
    }

    @Test
    @DisplayName("Should send alert batch to RabbitMQ exchange")
    void publishBatch_shouldSendToRabbit() {
        // Arrange
        AlertData alert1 = new AlertData(
                "abc1", 1L, "RULE1", "Rule 1", "CRITICAL",
                10, 20, LocalDateTime.now(), "INSTANT");
        AlertData alert2 = new AlertData(
                "abc2", 2L, "RULE2", "Rule 2", "HIGH",
                15, 20, LocalDateTime.now(), "INSTANT");
        java.util.List<AlertData> alerts = java.util.List.of(alert1, alert2);

        // Act
        publisher.publishBatch(alerts);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("alerts.exchange"), eq(""), eq(alerts));
    }
}
