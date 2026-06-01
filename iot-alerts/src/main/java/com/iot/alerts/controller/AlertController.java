package com.iot.alerts.controller;

import com.iot.alerts.domain.AlertEntity;
import com.iot.alerts.repository.AlertDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/alerts")
@Slf4j
@RequiredArgsConstructor
public class AlertController {

    private final AlertDataRepository alertDataRepository;

    @GetMapping
    public Flux<AlertEntity> getAlerts(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        log.debug("Fetching alerts, limit={}", limit);
        return alertDataRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(0, limit));
    }
}

