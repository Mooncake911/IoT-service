package com.iot.simulator.service;

import com.iot.shared.domain.DeviceData;
import com.iot.simulator.controller.DeviceGenerator;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SimulationService {

    private final DeviceGenerator deviceGenerator = new DeviceGenerator();
    private final WebClient webClient;

    @Value("${analytics.url}")
    private String analyticsUrl;

    private final AtomicInteger deviceCount;
    private final AtomicInteger messagesPerSecond;

    public int getDeviceCount() {
        return deviceCount.get();
    }

    public int getMessagesPerSecond() {
        return messagesPerSecond.get();
    }

    private Disposable simulationSubscription;
    private List<DeviceData> deviceData;

    public SimulationService(WebClient.Builder webClientBuilder,
            @Value("${app.simulation.device-count}") int deviceCount,
            @Value("${app.simulation.messages-per-second}") int messagesPerSecond) {
        this.webClient = webClientBuilder.build();
        this.deviceCount = new AtomicInteger(deviceCount);
        this.messagesPerSecond = new AtomicInteger(messagesPerSecond);
    }

    public Mono<Void> configure(int deviceCount, int messagesPerSecond) {
        return Mono.fromRunnable(() -> {
            log.info("Configuring simulation: deviceCount={}, messagesPerSecond={}", deviceCount, messagesPerSecond);
            this.deviceCount.set(deviceCount);
            this.messagesPerSecond.set(messagesPerSecond);
            this.deviceData = deviceGenerator.randomDevices(deviceCount);
        }).then();
    }

    public synchronized Mono<Void> start() {
        if (isRunning()) {
            return Mono.empty();
        }
        if (deviceData == null) {
            configure(deviceCount.get(), messagesPerSecond.get());
        }

        log.info("Starting simulation with {} devices at {} msg/s", deviceCount.get(), messagesPerSecond.get());
        long periodMs = 1000L / messagesPerSecond.get();

        simulationSubscription = Flux.interval(Duration.ofMillis(periodMs))
                .flatMap(tick -> sendData())
                .subscribe(
                        null,
                        error -> log.error("Critical error in simulation: {}", error.getMessage()));
        return Mono.empty();
    }

    public synchronized Mono<Void> stop() {
        return Mono.fromRunnable(() -> {
            if (simulationSubscription != null && !simulationSubscription.isDisposed()) {
                log.info("Stopping simulation");
                simulationSubscription.dispose();
            }
        }).then();
    }

    private Mono<Void> sendData() {
        if (deviceData != null) {
            this.deviceData = deviceData.stream()
                    .map(deviceGenerator::updateDevice)
                    .toList();
        }

        return webClient.post()
                .uri(analyticsUrl)
                .bodyValue(deviceData)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("Error sending data to analytics: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty()); // Continue stream even on error
    }

    public boolean isRunning() {
        return simulationSubscription != null && !simulationSubscription.isDisposed();
    }

}
