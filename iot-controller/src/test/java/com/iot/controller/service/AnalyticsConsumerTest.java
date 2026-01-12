package com.iot.controller.service;

import com.iot.controller.domain.AnalyticsEntity;
import com.iot.controller.repository.AnalyticsDataRepository;
import com.iot.shared.domain.AnalyticsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Analytics Consumer Communication Test")
public class AnalyticsConsumerTest {

    @Mock
    private AnalyticsDataRepository repository;

    @InjectMocks
    private AnalyticsConsumer consumer;

    @Test
    @DisplayName("Should receive analytics data and save it to repository")
    void consumeAnalyticsData_shouldSaveToRepo() {
        // Arrange
        AnalyticsData data = new AnalyticsData(1L, Instant.now(), Map.of("temp", 25.0));
        when(repository.save(any(AnalyticsEntity.class))).thenReturn(Mono.empty());

        // Act
        consumer.consumeAnalyticsData(data);

        // Assert
        verify(repository).save(any(AnalyticsEntity.class));
    }
}
