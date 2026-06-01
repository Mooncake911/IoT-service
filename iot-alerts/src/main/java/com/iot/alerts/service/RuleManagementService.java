package com.iot.alerts.service;

import com.iot.alerts.controller.dto.RuleResponse;
import com.iot.alerts.controller.dto.RuleUpsertRequest;
import com.iot.alerts.domain.RuleEntity;
import com.iot.alerts.model.Rule;
import com.iot.alerts.model.RuleType;
import com.iot.alerts.repository.RuleRepository;
import com.iot.contracts.domain.DeviceData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@Slf4j
public class RuleManagementService {

    private final RuleRepository ruleRepository;
    private final AtomicReference<List<Rule>> instantRulesCache = new AtomicReference<>(List.of());
    private final AtomicReference<List<Rule>> durationRulesCache = new AtomicReference<>(List.of());

    public RuleManagementService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @PostConstruct
    void init() {
        refreshRuntimeRules()
                .doOnSuccess(unused -> log.info("Runtime alert rules cache initialized"))
                .doOnError(e -> log.error("Failed to initialize runtime alert rules cache: {}", e.getMessage(), e))
                .subscribe();
    }

    public List<Rule> getInstantRules() {
        return instantRulesCache.get();
    }

    public List<Rule> getDurationRules() {
        return durationRulesCache.get();
    }

    public Flux<RuleResponse> listRules() {
        return ruleRepository.findAll().map(this::toResponse);
    }

    public Mono<RuleResponse> createRule(RuleUpsertRequest request) {
        return validateAndNormalize(request)
                .flatMap(valid -> ruleRepository.save(toEntity(valid)))
                .flatMap(saved -> refreshRuntimeRules().thenReturn(saved))
                .map(this::toResponse);
    }

    public Mono<RuleResponse> updateRule(String id, RuleUpsertRequest request) {
        return validateAndNormalize(request)
                .flatMap(valid -> ruleRepository.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Rule not found: " + id)))
                        .flatMap(existing -> {
                            RuleEntity updated = new RuleEntity(
                                    existing.id(),
                                    valid.name(),
                                    valid.type(),
                                    valid.severity(),
                                    valid.field(),
                                    valid.operator(),
                                    valid.thresholdNumber(),
                                    valid.thresholdBoolean(),
                                    valid.thresholdText(),
                                    normalizedRequiredPackets(valid),
                                    normalizedCooldown(valid),
                                    valid.enabled(),
                                    existing.createdAt(),
                                    Instant.now());
                            return ruleRepository.save(updated);
                        }))
                .flatMap(saved -> refreshRuntimeRules().thenReturn(saved))
                .map(this::toResponse);
    }

    public Mono<Void> deleteRule(String id) {
        return ruleRepository.deleteById(id)
                .then(refreshRuntimeRules());
    }

    public Mono<Void> refreshRuntimeRules() {
        return ruleRepository.findAllByEnabledTrue()
                .map(this::toRuntimeRule)
                .collectList()
                .doOnNext(dynamicRules -> {
                    List<Rule> dynamicInstant = dynamicRules.stream().filter(r -> r.type() == RuleType.INSTANT).toList();
                    List<Rule> dynamicDuration = dynamicRules.stream().filter(r -> r.type() == RuleType.DURATION).toList();
                    instantRulesCache.set(dynamicInstant);
                    durationRulesCache.set(dynamicDuration);
                })
                .then();
    }

    private Mono<RuleUpsertRequest> validateAndNormalize(RuleUpsertRequest request) {
        return Mono.fromCallable(() -> {
            ConditionField field = parseField(request.field());
            ConditionOperator operator = parseOperator(request.operator());
            validateThreshold(field, request);
            validateOperatorCompatibility(field, operator);

            if (request.type() == RuleType.INSTANT && request.requiredPackets() != 1) {
                return new RuleUpsertRequest(
                        request.name(),
                        request.type(),
                        request.severity(),
                        field.name(),
                        operator.name(),
                        request.thresholdNumber(),
                        request.thresholdBoolean(),
                        request.thresholdText(),
                        1,
                        normalizedCooldown(request),
                        request.enabled());
            }

            if (request.type() == RuleType.DURATION && request.requiredPackets() < 1) {
                throw new IllegalArgumentException("requiredPackets must be >= 1 for duration rule");
            }

            return new RuleUpsertRequest(
                    request.name(),
                    request.type(),
                    request.severity(),
                    field.name(),
                    operator.name(),
                    request.thresholdNumber(),
                    request.thresholdBoolean(),
                    request.thresholdText(),
                    request.requiredPackets(),
                    normalizedCooldown(request),
                    request.enabled());
        });
    }

    private int normalizedRequiredPackets(RuleUpsertRequest request) {
        return request.type() == RuleType.INSTANT ? 1 : request.requiredPackets();
    }

    private int normalizedCooldown(RuleUpsertRequest request) {
        return Math.max(0, request.cooldownSeconds());
    }

    private RuleEntity toEntity(RuleUpsertRequest request) {
        Instant now = Instant.now();
        return new RuleEntity(
                null,
                request.name(),
                request.type(),
                request.severity(),
                request.field(),
                request.operator(),
                request.thresholdNumber(),
                request.thresholdBoolean(),
                request.thresholdText(),
                normalizedRequiredPackets(request),
                normalizedCooldown(request),
                request.enabled(),
                now,
                now);
    }

    private RuleResponse toResponse(RuleEntity entity) {
        return new RuleResponse(
                entity.id(),
                entity.name(),
                entity.type(),
                entity.severity(),
                entity.field(),
                entity.operator(),
                entity.thresholdNumber(),
                entity.thresholdBoolean(),
                entity.thresholdText(),
                entity.requiredPackets(),
                entity.cooldownSeconds(),
                entity.enabled(),
                entity.createdAt(),
                entity.updatedAt());
    }

    private Rule toRuntimeRule(RuleEntity entity) {
        ConditionField field = parseField(entity.field());
        ConditionOperator operator = parseOperator(entity.operator());
        Object threshold = extractThreshold(field, entity.thresholdNumber(), entity.thresholdBoolean(), entity.thresholdText());

        Predicate<DeviceData> predicate = deviceData -> {
            Object currentValue = field.extract(deviceData);
            return operator.test(currentValue, threshold, field);
        };
        Function<DeviceData, Object> extractor = field::extract;

        if (entity.type() == RuleType.DURATION) {
            return Rule.duration(
                    entity.id(),
                    entity.name(),
                    entity.severity(),
                    predicate,
                    extractor,
                    threshold,
                    entity.requiredPackets(),
                    entity.cooldownSeconds());
        }

        return Rule.instant(
                entity.id(),
                entity.name(),
                entity.severity(),
                predicate,
                extractor,
                threshold,
                entity.cooldownSeconds());
    }

    private void validateThreshold(ConditionField field, RuleUpsertRequest request) {
        extractThreshold(field, request.thresholdNumber(), request.thresholdBoolean(), request.thresholdText());
    }

    private Object extractThreshold(ConditionField field, Double thresholdNumber, Boolean thresholdBoolean, String thresholdText) {
        return switch (field) {
            case BATTERY_LEVEL, SIGNAL_STRENGTH -> {
                if (thresholdNumber == null || thresholdBoolean != null || (thresholdText != null && !thresholdText.isBlank())) {
                    throw new IllegalArgumentException("Numeric field requires thresholdNumber only");
                }
                yield thresholdNumber;
            }
            case IS_ONLINE -> {
                if (thresholdBoolean == null || thresholdNumber != null || (thresholdText != null && !thresholdText.isBlank())) {
                    throw new IllegalArgumentException("Boolean field requires thresholdBoolean only");
                }
                yield thresholdBoolean;
            }
            case DEVICE_NAME, MANUFACTURER, DEVICE_TYPE -> {
                if (thresholdText == null || thresholdText.isBlank() || thresholdNumber != null || thresholdBoolean != null) {
                    throw new IllegalArgumentException("Text field requires thresholdText only");
                }
                yield thresholdText.trim();
            }
        };
    }

    private ConditionField parseField(String field) {
        try {
            return ConditionField.valueOf(field.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported field: " + field);
        }
    }

    private ConditionOperator parseOperator(String operator) {
        try {
            return ConditionOperator.valueOf(operator.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private void validateOperatorCompatibility(ConditionField field, ConditionOperator operator) {
        switch (operator) {
            case LT, LTE, GT, GTE -> {
                if (!(field == ConditionField.BATTERY_LEVEL || field == ConditionField.SIGNAL_STRENGTH)) {
                    throw new IllegalArgumentException(operator + " operator supports only numeric fields");
                }
            }
            case CONTAINS -> {
                if (!(field == ConditionField.DEVICE_NAME || field == ConditionField.MANUFACTURER || field == ConditionField.DEVICE_TYPE)) {
                    throw new IllegalArgumentException("CONTAINS operator supports only text fields");
                }
            }
            case EQ, NEQ -> {
                // supported for any field type
            }
        }
    }

    private enum ConditionField {
        BATTERY_LEVEL {
            @Override
            Object extract(DeviceData deviceData) {
                return deviceData != null && deviceData.status() != null ? deviceData.status().batteryLevel() : null;
            }
        },
        SIGNAL_STRENGTH {
            @Override
            Object extract(DeviceData deviceData) {
                return deviceData != null && deviceData.status() != null ? deviceData.status().signalStrength() : null;
            }
        },
        DEVICE_NAME {
            @Override
            Object extract(DeviceData deviceData) {
                return deviceData != null ? deviceData.name() : null;
            }
        },
        MANUFACTURER {
            @Override
            Object extract(DeviceData deviceData) {
                return deviceData != null ? deviceData.manufacturer() : null;
            }
        },
        DEVICE_TYPE {
            @Override
            Object extract(DeviceData deviceData) {
                return deviceData != null && deviceData.type() != null ? deviceData.type().name() : null;
            }
        },
        IS_ONLINE {
            @Override
            Object extract(DeviceData deviceData) {
                return deviceData != null && deviceData.status() != null ? deviceData.status().isOnline() : null;
            }
        };

        abstract Object extract(DeviceData deviceData);
    }

    private enum ConditionOperator {
        LT {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                requireNumericField(field, "LT");
                return toDouble(value) < toDouble(threshold);
            }
        },
        LTE {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                requireNumericField(field, "LTE");
                return toDouble(value) <= toDouble(threshold);
            }
        },
        GT {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                requireNumericField(field, "GT");
                return toDouble(value) > toDouble(threshold);
            }
        },
        GTE {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                requireNumericField(field, "GTE");
                return toDouble(value) >= toDouble(threshold);
            }
        },
        EQ {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                return safeEquals(value, threshold);
            }
        },
        NEQ {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                return !safeEquals(value, threshold);
            }
        },
        CONTAINS {
            @Override
            boolean test(Object value, Object threshold, ConditionField field) {
                requireTextField(field, "CONTAINS");
                if (value == null || threshold == null) {
                    return false;
                }
                return value.toString().toLowerCase().contains(threshold.toString().toLowerCase());
            }
        };

        abstract boolean test(Object value, Object threshold, ConditionField field);

        double toDouble(Object value) {
            if (value instanceof Number n) {
                return n.doubleValue();
            }
            throw new IllegalArgumentException("Operator requires numeric value");
        }

        boolean safeEquals(Object left, Object right) {
            if (left == null) {
                return right == null;
            }
            return left.equals(right);
        }

        void requireNumericField(ConditionField field, String operator) {
            if (!(field == ConditionField.BATTERY_LEVEL || field == ConditionField.SIGNAL_STRENGTH)) {
                throw new IllegalArgumentException(operator + " operator supports only numeric fields");
            }
        }

        void requireTextField(ConditionField field, String operator) {
            if (!(field == ConditionField.DEVICE_NAME || field == ConditionField.MANUFACTURER || field == ConditionField.DEVICE_TYPE)) {
                throw new IllegalArgumentException(operator + " operator supports only text fields");
            }
        }
    }
}
