package com.iot.simulator.controller;

import com.iot.simulator.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulationService simulationService;

    public SimulatorController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/config")
    public void configure(@RequestBody Map<String, Object> config) {
        int deviceCount = (int) config.getOrDefault("deviceCount", 10);
        int messagesPerSecond = (int) config.getOrDefault("messagesPerSecond", 1);
        simulationService.configure(deviceCount, messagesPerSecond);
    }

    @PostMapping("/start")
    public void start() {
        simulationService.start();
    }

    @PostMapping("/stop")
    public void stop() {
        simulationService.stop();
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        LinkedHashMap<String, Object> status = new LinkedHashMap<>();
        status.put("running", simulationService.isRunning());
        status.put("deviceCount", simulationService.getDeviceCount());
        status.put("messagesPerSecond", simulationService.getMessagesPerSecond());
        return status;
    }
}
