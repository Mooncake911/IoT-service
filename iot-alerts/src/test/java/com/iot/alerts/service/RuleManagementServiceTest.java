package com.iot.alerts.service;

import com.iot.alerts.controller.dto.RuleResponse;
import com.iot.alerts.controller.dto.RuleUpsertRequest;
import com.iot.alerts.domain.RuleEntity;
import com.iot.alerts.model.RuleType;
import com.iot.alerts.model.Severity;
import com.iot.alerts.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleManagementService Tests")
class RuleManagementServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    private RuleManagementService service;

    @BeforeEach
    void setUp() {
        service = new RuleManagementService(ruleRepository);
        lenient().when(ruleRepository.findAllByEnabledTrue()).thenReturn(Flux.empty());
    }

    private static RuleUpsertRequest numericInstant(int requiredPackets, int cooldown) {
        return new RuleUpsertRequest("low-battery", RuleType.INSTANT, Severity.WARNING,
                "BATTERY_LEVEL", "LT", 20.0, null, null, requiredPackets, cooldown, true);
    }

    private static RuleEntity entity(String id, RuleType type, boolean enabled) {
        Instant now = Instant.now();
        return new RuleEntity(id, "low-battery", type, Severity.WARNING,
                "BATTERY_LEVEL", "LT", 20.0, null, null, 3, 30, enabled, now, now);
    }

    @Test
    @DisplayName("listRules maps entities to responses")
    void listRules_shouldMapEntities() {
        when(ruleRepository.findAll()).thenReturn(Flux.just(entity("1", RuleType.INSTANT, true)));

        StepVerifier.create(service.listRules())
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo("1");
                    assertThat(r.name()).isEqualTo("low-battery");
                    assertThat(r.type()).isEqualTo(RuleType.INSTANT);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createRule normalizes INSTANT requiredPackets to 1 and negative cooldown to 0")
    void createRule_shouldNormalizeInstant() {
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.createRule(numericInstant(5, -10)))
                .assertNext(r -> {
                    assertThat(r.requiredPackets()).isEqualTo(1);
                    assertThat(r.cooldownSeconds()).isEqualTo(0);
                })
                .verifyComplete();

        ArgumentCaptor<RuleEntity> captor = ArgumentCaptor.forClass(RuleEntity.class);
        verify(ruleRepository).save(captor.capture());
        assertThat(captor.getValue().requiredPackets()).isEqualTo(1);
        assertThat(captor.getValue().cooldownSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("createRule rejects DURATION with requiredPackets < 1")
    void createRule_shouldRejectDurationWithoutPackets() {
        RuleUpsertRequest req = new RuleUpsertRequest("r", RuleType.DURATION, Severity.CRITICAL,
                "BATTERY_LEVEL", "LT", 20.0, null, null, 0, 30, true);

        StepVerifier.create(service.createRule(req))
                .expectErrorMessage("requiredPackets must be >= 1 for duration rule")
                .verify();
    }

    @Test
    @DisplayName("createRule rejects unknown field and operator")
    void createRule_shouldRejectUnknownFieldAndOperator() {
        RuleUpsertRequest badField = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "NOPE", "LT", 20.0, null, null, 1, 0, true);
        StepVerifier.create(service.createRule(badField))
                .expectErrorMessage("Unsupported field: NOPE")
                .verify();

        RuleUpsertRequest badOp = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "BATTERY_LEVEL", "NOPE", 20.0, null, null, 1, 0, true);
        StepVerifier.create(service.createRule(badOp))
                .expectErrorMessage("Unsupported operator: NOPE")
                .verify();
    }

    @Test
    @DisplayName("createRule validates threshold shape per field kind")
    void createRule_shouldValidateThresholdShape() {
        // numeric field with text threshold
        RuleUpsertRequest numericWithText = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "BATTERY_LEVEL", "LT", null, null, "abc", 1, 0, true);
        StepVerifier.create(service.createRule(numericWithText))
                .expectErrorMessage("Numeric field requires thresholdNumber only")
                .verify();

        // boolean field with number threshold
        RuleUpsertRequest booleanWithNumber = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "IS_ONLINE", "EQ", 1.0, null, null, 1, 0, true);
        StepVerifier.create(service.createRule(booleanWithNumber))
                .expectErrorMessage("Boolean field requires thresholdBoolean only")
                .verify();

        // text field without text
        RuleUpsertRequest textBlank = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "DEVICE_NAME", "EQ", null, null, "  ", 1, 0, true);
        StepVerifier.create(service.createRule(textBlank))
                .expectErrorMessage("Text field requires thresholdText only")
                .verify();
    }

    @Test
    @DisplayName("createRule rejects incompatible operator/field pairs")
    void createRule_shouldRejectIncompatibleOperator() {
        RuleUpsertRequest ltOnText = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "DEVICE_NAME", "LT", null, null, "abc", 1, 0, true);
        StepVerifier.create(service.createRule(ltOnText))
                .expectErrorMessage("LT operator supports only numeric fields")
                .verify();

        RuleUpsertRequest containsOnNumeric = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.WARNING,
                "BATTERY_LEVEL", "CONTAINS", 20.0, null, null, 1, 0, true);
        StepVerifier.create(service.createRule(containsOnNumeric))
                .expectErrorMessage("CONTAINS operator supports only text fields")
                .verify();
    }

    @Test
    @DisplayName("createRule accepts boolean and text rules")
    void createRule_shouldAcceptBooleanAndText() {
        RuleUpsertRequest online = new RuleUpsertRequest("r", RuleType.INSTANT, Severity.CRITICAL,
                "is_online", "eq", null, true, null, 1, 5, true);
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.createRule(online))
                .assertNext(r -> {
                    assertThat(r.field()).isEqualTo("IS_ONLINE");
                    assertThat(r.operator()).isEqualTo("EQ");
                })
                .verifyComplete();

        RuleUpsertRequest text = new RuleUpsertRequest("r", RuleType.DURATION, Severity.WARNING,
                "manufacturer", "contains", null, null, "  Acme  ", 3, 5, true);
        StepVerifier.create(service.createRule(text))
                .assertNext(r -> assertThat(r.thresholdText()).isEqualTo("  Acme  "))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateRule fails for unknown id")
    void updateRule_shouldFailForUnknownId() {
        when(ruleRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.updateRule("missing", numericInstant(1, 0)))
                .expectErrorMessage("Rule not found: missing")
                .verify();
    }

    @Test
    @DisplayName("updateRule saves merged entity preserving id and createdAt")
    void updateRule_shouldSaveMerged() {
        RuleEntity existing = entity("42", RuleType.DURATION, true);
        when(ruleRepository.findById("42")).thenReturn(Mono.just(existing));
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.updateRule("42", numericInstant(1, 7)))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo("42");
                    assertThat(r.createdAt()).isEqualTo(existing.createdAt());
                    assertThat(r.cooldownSeconds()).isEqualTo(7);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteRule deletes and refreshes cache")
    void deleteRule_shouldDeleteAndRefresh() {
        when(ruleRepository.deleteById("9")).thenReturn(Mono.empty());

        StepVerifier.create(service.deleteRule("9")).verifyComplete();
        verify(ruleRepository).deleteById("9");
    }

    @Test
    @DisplayName("refreshRuntimeRules splits instant and duration caches")
    void refreshRuntimeRules_shouldSplitCaches() {
        when(ruleRepository.findAllByEnabledTrue()).thenReturn(Flux.just(
                entity("i1", RuleType.INSTANT, true),
                entity("d1", RuleType.DURATION, true)));

        StepVerifier.create(service.refreshRuntimeRules()).verifyComplete();

        assertThat(service.getInstantRules()).hasSize(1);
        assertThat(service.getDurationRules()).hasSize(1);
        assertThat(service.getInstantRules().get(0).id()).isEqualTo("i1");
    }

    @Test
    @DisplayName("init loads caches on startup")
    void init_shouldLoadCaches() {
        when(ruleRepository.findAllByEnabledTrue()).thenReturn(Flux.just(entity("i1", RuleType.INSTANT, true)));

        service.init();

        assertThat(service.getInstantRules()).hasSize(1);
    }

    @Test
    @DisplayName("toResponse maps all fields")
    void listRules_shouldMapAllFields() {
        Instant now = Instant.now();
        RuleEntity full = new RuleEntity("7", "n", RuleType.DURATION, Severity.CRITICAL,
                "IS_ONLINE", "EQ", null, true, null, 3, 11, false, now, now);
        when(ruleRepository.findAll()).thenReturn(Flux.just(full));

        StepVerifier.create(service.listRules())
                .assertNext(r -> assertThat(r).isEqualTo(new RuleResponse("7", "n", RuleType.DURATION,
                        Severity.CRITICAL, "IS_ONLINE", "EQ", null, true, null, 3, 11, false, now, now)))
                .verifyComplete();
    }
}
