package com.iot.analytics.service;

import com.iot.shared.domain.AnalyticsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Analytics Publisher Communication Test")
public class AnalyticsPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AnalyticsPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "analyticsExchangeName", "analytics.exchange");
    }

    @Test
    @DisplayName("Should send analytics data to RabbitMQ exchange")
    void publish_shouldSendToRabbit() {
        // Arrange
        AnalyticsData data = new AnalyticsData(1L, Instant.now(), Map.of("avgBatt", 80.0));

        // Act
        publisher.publish(data);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("analytics.exchange"), eq(""), eq(data));
    }
}
