package com.iot.controller.service;

import com.iot.controller.domain.AlertEntity;
import com.iot.controller.repository.AlertDataRepository;
import com.iot.shared.domain.AlertData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Alert Consumer Communication Test")
public class AlertConsumerTest {

    @Mock
    private AlertDataRepository repository;

    @InjectMocks
    private AlertConsumer consumer;

    @Test
    @DisplayName("Should receive alert and save it to repository")
    void consumeAlert_shouldSaveToRepo() {
        // Arrange
        AlertData alert = new AlertData(
                "alert1", 1L, "rule1", "Rule 1", "HIGH",
                30, 20, LocalDateTime.now(), "INSTANT");
        when(repository.save(any(AlertEntity.class))).thenReturn(Mono.empty());

        // Act
        consumer.consumeAlert(alert);

        // Assert
        verify(repository).save(any(AlertEntity.class));
    }
}
