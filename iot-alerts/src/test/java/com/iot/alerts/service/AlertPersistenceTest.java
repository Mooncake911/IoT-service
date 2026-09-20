package com.iot.alerts.service;

import com.iot.alerts.domain.AlertEntity;
import com.iot.alerts.repository.AlertDataRepository;
import com.iot.contracts.domain.AlertData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertPersistence Tests")
class AlertPersistenceTest {

    @Mock
    private AlertDataRepository repository;

    private AlertData alert() {
        return AlertData.builder()
                .alertId("a1")
                .deviceId(101L)
                .ruleId("r1")
                .ruleName("low-battery")
                .severity("WARNING")
                .currentValue(3)
                .threshold(20)
                .timestamp(Instant.now())
                .ruleType("INSTANT")
                .build();
    }

    @Test
    @DisplayName("save maps AlertData to entity and persists")
    void save_shouldPersistMappedEntity() {
        AlertPersistence persistence = new AlertPersistence(repository);
        when(repository.save(any(AlertEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(persistence.save(alert())).verifyComplete();

        ArgumentCaptor<AlertEntity> captor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(repository).save(captor.capture());
        AlertEntity saved = captor.getValue();
        assertThat(saved.deviceId()).isEqualTo(101L);
        assertThat(saved.ruleId()).isEqualTo("r1");
        assertThat(saved.ruleName()).isEqualTo("low-battery");
        assertThat(saved.receivedAt()).isNotNull();
    }

    @Test
    @DisplayName("save propagates repository errors")
    void save_shouldPropagateErrors() {
        AlertPersistence persistence = new AlertPersistence(repository);
        when(repository.save(any(AlertEntity.class))).thenReturn(Mono.error(new RuntimeException("db down")));

        StepVerifier.create(persistence.save(alert()))
                .expectErrorMessage("db down")
                .verify();
    }

    @Test
    @DisplayName("saveBatch ignores null and empty lists")
    void saveBatch_shouldIgnoreNullAndEmpty() {
        AlertPersistence persistence = new AlertPersistence(repository);

        StepVerifier.create(persistence.saveBatch(null)).verifyComplete();
        StepVerifier.create(persistence.saveBatch(List.of())).verifyComplete();

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("saveBatch persists all mapped entities")
    void saveBatch_shouldPersistAll() {
        AlertPersistence persistence = new AlertPersistence(repository);
        when(repository.saveAll(any(List.class))).thenReturn(Flux.empty());

        StepVerifier.create(persistence.saveBatch(List.of(alert(), alert()))).verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlertEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }
}
