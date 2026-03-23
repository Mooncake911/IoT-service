package com.iot.controller.service;

import com.iot.controller.repository.AnalyticsDataRepository;
import com.iot.shared.domain.AnalyticsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsConsumerTest {

    @Mock
    private AnalyticsDataRepository repository;

    private AnalyticsConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AnalyticsConsumer(repository, 2, 1000, 10, 1);
    }

    @Test
    void consumeAnalytics_shouldBatchSaves() throws InterruptedException {
        // Arrange
        AnalyticsData data1 = new AnalyticsData(1L, Instant.now(), Map.of("cpu", 10.0));
        AnalyticsData data2 = new AnalyticsData(2L, Instant.now(), Map.of("cpu", 20.0));

        List<AnalyticsData> incomingBatch = List.of(data1, data2);

        when(repository.saveAll(anyList())).thenReturn(Flux.empty());

        // Act
        consumer.consumeAnalytics(incomingBatch);

        // Assert - wait a bit for the reactive pipeline to process
        // Since batch size is 2, it should trigger immediately
        Thread.sleep(100);

        verify(repository, times(1)).saveAll(anyList());
    }
}
