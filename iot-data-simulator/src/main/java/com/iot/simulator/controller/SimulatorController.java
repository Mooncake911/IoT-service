package com.iot.simulator.controller;

import com.iot.simulator.service.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/simulator")
@Slf4j
@RequiredArgsConstructor
public class SimulatorController {

    private final SimulationService simulationService;

    @PostMapping("/config")
    public Mono<String> configure(@RequestParam int deviceCount, @RequestParam int messagesPerSecond) {
        log.info("Updating simulator config: deviceCount={}, messagesPerSecond={}", deviceCount, messagesPerSecond);
        return simulationService.configure(deviceCount, messagesPerSecond)
                .then(Mono.just("Config changed to: " + deviceCount + " (msg/s: " + messagesPerSecond + ")"));
    }

    @PostMapping("/start")
    public Mono<Void> start() {
        log.info("Request to start simulation");
        return simulationService.start();
    }

    @PostMapping("/stop")
    public Mono<Void> stop() {
        log.info("Request to stop simulation");
        return simulationService.stop();
    }

    @GetMapping("/status")
    public Mono<Map<String, Object>> getStatus() {
        return Mono.fromCallable(() -> {
            LinkedHashMap<String, Object> status = new LinkedHashMap<>();
            status.put("running", simulationService.isRunning());
            status.put("deviceCount", simulationService.getDeviceCount());
            status.put("messagesPerSecond", simulationService.getMessagesPerSecond());
            return status;
        });
    }
}
