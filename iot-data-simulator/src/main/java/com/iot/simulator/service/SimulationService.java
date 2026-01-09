package com.iot.simulator.service;

import com.iot.shared.domain.Device;
import com.iot.simulator.controller.DeviceGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class SimulationService {

    private final DeviceGenerator deviceGenerator = new DeviceGenerator();
    private final WebClient webClient;

    @Value("${analytics.url}")
    private String analyticsUrl;

    private int deviceCount = 10;
    private int messagesPerSecond = 1;

    public int getDeviceCount() {
        return deviceCount;
    }

    public int getMessagesPerSecond() {
        return messagesPerSecond;
    }

    private Disposable simulationSubscription;
    private List<Device> devices;

    public SimulationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public synchronized void configure(int deviceCount, int messagesPerSecond) {
        this.deviceCount = deviceCount;
        this.messagesPerSecond = messagesPerSecond;
        this.devices = deviceGenerator.randomDevices(deviceCount);
    }

    public synchronized void start() {
        if (isRunning()) {
            return;
        }
        if (devices == null) {
            configure(deviceCount, messagesPerSecond);
        }

        long periodMs = 1000L / messagesPerSecond;

        simulationSubscription = Flux.interval(Duration.ofMillis(periodMs))
                .flatMap(tick -> sendData())
                .subscribe(
                        null,
                        error -> System.err.println("Critical error in simulation: " + error.getMessage()));
    }

    public synchronized void stop() {
        if (simulationSubscription != null && !simulationSubscription.isDisposed()) {
            simulationSubscription.dispose();
        }
    }

    private Mono<Void> sendData() {
        if (devices != null) {
            devices.forEach(deviceGenerator::updateDevice);
        }

        return webClient.post()
                .uri(analyticsUrl)
                .bodyValue(devices)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> System.err.println("Error sending data to analytics: " + e.getMessage()))
                .onErrorResume(e -> Mono.empty()); // Continue stream even on error
    }

    public boolean isRunning() {
        return simulationSubscription != null && !simulationSubscription.isDisposed();
    }

}
