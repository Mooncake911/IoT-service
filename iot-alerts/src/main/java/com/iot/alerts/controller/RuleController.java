package com.iot.alerts.controller;

import com.iot.alerts.controller.dto.RuleResponse;
import com.iot.alerts.controller.dto.RuleUpsertRequest;
import com.iot.alerts.service.RuleManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts/rules")
@Slf4j
@RequiredArgsConstructor
public class RuleController {

    private final RuleManagementService ruleManagementService;

    @GetMapping
    public Flux<RuleResponse> getRules() {
        return ruleManagementService.listRules();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RuleResponse> createRule(@Valid @RequestBody RuleUpsertRequest request) {
        log.info("Creating alert rule '{}'", request.name());
        return ruleManagementService.createRule(request);
    }

    @PutMapping("/{id}")
    public Mono<RuleResponse> updateRule(@PathVariable String id, @Valid @RequestBody RuleUpsertRequest request) {
        log.info("Updating alert rule {} ('{}')", id, request.name());
        return ruleManagementService.updateRule(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRule(@PathVariable String id) {
        log.info("Deleting alert rule {}", id);
        return ruleManagementService.deleteRule(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return Mono.just(Map.of("error", "bad_request", "message", ex.getMessage()));
    }
}
