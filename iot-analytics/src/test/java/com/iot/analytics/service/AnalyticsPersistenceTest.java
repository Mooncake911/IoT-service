package com.iot.analytics.service;

import com.iot.analytics.domain.AnalyticsEntity;
import com.iot.analytics.repository.AnalyticsDataRepository;
import com.iot.contracts.domain.AnalyticsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsPersistence Tests")
class AnalyticsPersistenceTest {

    @Mock
    private AnalyticsDataRepository repository;

    @Test
    @DisplayName("save maps AnalyticsData to entity and persists")
    void save_shouldPersistMappedEntity() {
        AnalyticsPersistence persistence = new AnalyticsPersistence(repository);
        Instant now = Instant.now();
        when(repository.save(any(AnalyticsEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AnalyticsData data = AnalyticsData.builder()
                .timestamp(now)
                .metrics(Map.of("totalDevices", 3.0))
                .build();

        StepVerifier.create(persistence.save(data)).verifyComplete();

        ArgumentCaptor<AnalyticsEntity> captor = ArgumentCaptor.forClass(AnalyticsEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().timestamp()).isEqualTo(now);
        assertThat(captor.getValue().metrics()).containsEntry("totalDevices", 3.0);
    }

    @Test
    @DisplayName("save propagates repository errors")
    void save_shouldPropagateErrors() {
        AnalyticsPersistence persistence = new AnalyticsPersistence(repository);
        when(repository.save(any(AnalyticsEntity.class))).thenReturn(Mono.error(new RuntimeException("db down")));

        AnalyticsData data = AnalyticsData.builder()
                .timestamp(Instant.now())
                .metrics(Map.of("totalDevices", 1.0))
                .build();

        StepVerifier.create(persistence.save(data))
                .expectErrorMessage("db down")
                .verify();
    }
}
