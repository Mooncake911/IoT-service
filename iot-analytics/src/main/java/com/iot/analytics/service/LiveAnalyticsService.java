package com.iot.analytics.service;

import com.iot.contracts.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class LiveAnalyticsService {

    private final AtomicReference<Map<String, Object>> summaryRef = new AtomicReference<>(defaultSummary());
    private final AtomicReference<Map<String, Object>> byTypeRef = new AtomicReference<>(Map.of("types", Map.of()));
    private final ConcurrentHashMap<Long, Boolean> lastOnlineByDevice = new ConcurrentHashMap<>();

    public void ingestBatch(List<DeviceData> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        long total = batch.size();
        long online = 0;
        long offline = 0;
        long lowBattery = 0;
        long weakSignal = 0;
        long batterySum = 0;
        long signalSum = 0;
        long flaps = 0;
        Map<String, Long> typeDistribution = new LinkedHashMap<>();

        for (DeviceData device : batch) {
            if (device.type() != null) {
                typeDistribution.merge(device.type().name(), 1L, Long::sum);
            }
            if (device.status() == null) {
                continue;
            }

            boolean isOnline = device.status().isOnline();
            if (isOnline) {
                online++;
            } else {
                offline++;
            }
            batterySum += device.status().batteryLevel();
            signalSum += device.status().signalStrength();
            if (device.status().batteryLevel() < 20) {
                lowBattery++;
            }
            if (device.status().signalStrength() < 25) {
                weakSignal++;
            }

            Boolean prev = lastOnlineByDevice.put(device.id(), isOnline);
            if (prev != null && prev != isOnline) {
                flaps++;
            }
        }

        double totalD = (double) total;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timestamp", Instant.now().toString());
        summary.put("windowSizeDevices", total);
        summary.put("ingestRateApproxPerSec", totalD);
        summary.put("onlineRate", online / totalD);
        summary.put("offlineCount", offline);
        summary.put("avgBatteryLevel", batterySum / totalD);
        summary.put("avgSignalStrength", signalSum / totalD);
        summary.put("lowBatteryCount", lowBattery);
        summary.put("weakSignalCount", weakSignal);
        summary.put("flappingCount", flaps);

        Map<String, Object> byType = new LinkedHashMap<>();
        byType.put("timestamp", Instant.now().toString());
        byType.put("types", typeDistribution);

        summaryRef.set(summary);
        byTypeRef.set(byType);
        log.debug("Live analytics updated: windowSize={}", total);
    }

    public Map<String, Object> getSummary() {
        return summaryRef.get();
    }

    public Map<String, Object> getByType() {
        return byTypeRef.get();
    }

    private static Map<String, Object> defaultSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timestamp", Instant.now().toString());
        summary.put("windowSizeDevices", 0L);
        summary.put("ingestRateApproxPerSec", 0.0);
        summary.put("onlineRate", 0.0);
        summary.put("offlineCount", 0L);
        summary.put("avgBatteryLevel", 0.0);
        summary.put("avgSignalStrength", 0.0);
        summary.put("lowBatteryCount", 0L);
        summary.put("weakSignalCount", 0L);
        summary.put("flappingCount", 0L);
        return summary;
    }
}
