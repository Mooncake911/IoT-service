package com.iot.simulator.service;

import com.iot.simulator.controller.DeviceGenerator;
import com.iot.contracts.domain.DeviceData;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class SimulationService {

    private final DeviceGenerator deviceGenerator = new DeviceGenerator();
    private final WebClient webClient;

    @Value("${controller.url}")
    private String controllerUrl;

    private final AtomicInteger deviceCount;
    private final AtomicInteger frequencySeconds;
    private final AtomicInteger batchSize;

    public int getDeviceCount() {
        return deviceCount.get();
    }

    public int getFrequencySeconds() {
        return frequencySeconds.get();
    }

    public int getBatchSize() {
        return batchSize.get();
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Sinks.Many<Boolean> controlSink = Sinks.many().multicast().onBackpressureBuffer();
    private List<DeviceData> deviceData;

    public SimulationService(
            WebClient.Builder webClientBuilder,
            @Value("${app.simulation.device-count}") int deviceCount,
            @Value("${app.simulation.frequency-seconds}") int frequencySeconds,
            @Value("${app.simulation.batch-size}") int batchSize) {
        this.webClient = webClientBuilder.build();
        this.deviceCount = new AtomicInteger(deviceCount);
        this.frequencySeconds = new AtomicInteger(frequencySeconds);
        this.batchSize = new AtomicInteger(batchSize);

        // Declarative simulation pipeline
        controlSink.asFlux()
                .distinctUntilChanged()
                .switchMap(run -> {
                    if (!run) {
                        return Flux.empty();
                    }
                    return Flux.interval(Duration.ofSeconds(1))
                            .onBackpressureDrop(tick -> log.warn("Simulation tick dropped due to slow processing"))
                            .concatMap(tick -> {
                                int currentTotal = this.deviceCount.get();
                                int freq = this.frequencySeconds.get();
                                int devicesToSendPerSecond = Math.max(1, currentTotal / freq);
                                return sendData(devicesToSendPerSecond);
                            });
                })
                .subscribe(
                        null,
                        error -> log.error("Critical error in simulation pipeline: {}", error.getMessage()),
                        () -> log.info("Simulation pipeline completed")
                );
    }

    public Mono<Void> configure(int deviceCount, int frequencySeconds) {
        return Mono.fromRunnable(() -> {
            log.info("Configuring simulation: deviceCount={}, frequencySeconds={}",
                    deviceCount, frequencySeconds);
            this.deviceCount.set(deviceCount);
            this.frequencySeconds.set(frequencySeconds);
            this.deviceData = deviceGenerator.randomDevices(deviceCount);
        });
    }

    public Mono<Void> start() {
        return (deviceData == null ? configure(deviceCount.get(), frequencySeconds.get()) : Mono.empty())
                .then(Mono.fromRunnable(() -> {
                    if (running.compareAndSet(false, true)) {
                        log.info("Starting simulation: {} devices, frequency 1/{}s", deviceCount.get(),
                                frequencySeconds.get());
                        controlSink.tryEmitNext(true);
                    }
                }));
    }

    public Mono<Void> stop() {
        return Mono.fromRunnable(() -> {
            if (running.compareAndSet(true, false)) {
                log.info("Stopping simulation");
                controlSink.tryEmitNext(false);
            }
        });
    }

    private Mono<Void> sendData(int count) {
        if (deviceData == null || deviceData.isEmpty()) {
            return Mono.empty();
        }

        List<DeviceData> toSend = deviceData.stream()
                .limit(count)
                .map(deviceGenerator::updateDevice)
                .toList();

        if (!toSend.isEmpty()) {
            log.debug("Sending stats for device IDs: {}", toSend.stream().map(DeviceData::id).toList());
        }

        // Split into batches and send sequentially for backpressure
        return Flux.fromIterable(toSend)
                .buffer(batchSize.get())
                .concatMap(batch -> {
                    log.debug("Sending batch of {} devices", batch.size());
                    return webClient.post()
                            .uri(controllerUrl)
                            .bodyValue(batch)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .retry(1)
                            .doOnError(e -> log.error("Error sending batch to controller: {}", e.getMessage()))
                            .onErrorResume(e -> Mono.empty());
                })
                .then();
    }

    public boolean isRunning() {
        return running.get();
    }

}
