package com.iot.controller.service;

import com.iot.controller.repository.AlertDataRepository;
import com.iot.shared.domain.AlertData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertConsumerTest {

    @Mock
    private AlertDataRepository repository;

    private AlertConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AlertConsumer(repository, 2, 1000, 10, 1);
    }

    @Test
    void consumeAlert_shouldBatchSaves() throws InterruptedException {
        // Arrange
        AlertData data1 = new AlertData("id1", 1L, "rule1", "Rule 1", "HIGH", 10.0, 5.0, LocalDateTime.now(),
                "INSTANT");
        AlertData data2 = new AlertData("id2", 2L, "rule2", "Rule 2", "LOW", 3.0, 5.0, LocalDateTime.now(), "INSTANT");

        List<AlertData> incomingBatch = List.of(data1, data2);

        when(repository.saveAll(anyList())).thenReturn(Flux.empty());

        // Act
        consumer.consumeAlert(incomingBatch);

        // Assert
        Thread.sleep(100);

        verify(repository, times(1)).saveAll(anyList());
    }
}
