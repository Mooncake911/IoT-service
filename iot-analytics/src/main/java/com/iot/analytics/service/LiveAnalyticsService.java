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

    private final ConcurrentHashMap<Long, DeviceData> latestDevices = new ConcurrentHashMap<>();
    private final AtomicReference<Map<String, Object>> summaryRef = new AtomicReference<>(defaultSummary());
    private final AtomicReference<Map<String, Object>> byTypeRef = new AtomicReference<>(Map.of("types", Map.of()));
    private final AtomicReference<Map<String, Object>> byManufacturerRef = new AtomicReference<>(Map.of("manufacturers", Map.of()));
    private final ConcurrentHashMap<Long, Boolean> lastOnlineByDevice = new ConcurrentHashMap<>();

    public synchronized void ingestDevices(List<DeviceData> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        for (DeviceData device : batch) {
            ingestDevice(device);
        }
        recompute();
    }

    private void ingestDevice(DeviceData device) {
        if (device == null) {
            return;
        }
        latestDevices.put(device.id(), device);
    }

    private void recompute() {
        List<DeviceData> devices = List.copyOf(latestDevices.values());
        long total = devices.size();
        long online = 0;
        long offline = 0;
        long lowBattery = 0;
        long weakSignal = 0;
        long batterySum = 0;
        long signalSum = 0;
        long flaps = 0;
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        Map<String, Long> manufacturerDistribution = new LinkedHashMap<>();

        for (DeviceData device : devices) {
            if (device.type() != null) {
                typeDistribution.merge(device.type().name(), 1L, Long::sum);
            }
            if (device.manufacturer() != null) {
                manufacturerDistribution.merge(device.manufacturer(), 1L, Long::sum);
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

        double totalD = total == 0 ? 1.0 : (double) total;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timestamp", Instant.now().toString());
        summary.put("totalUniqueDevices", total);
        summary.put("onlineCount", online);
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

        Map<String, Object> byManufacturer = new LinkedHashMap<>();
        byManufacturer.put("timestamp", Instant.now().toString());
        byManufacturer.put("manufacturers", manufacturerDistribution);

        summaryRef.set(summary);
        byTypeRef.set(byType);
        byManufacturerRef.set(byManufacturer);
        log.debug("Live analytics updated: totalUniqueDevices={}", total);
    }

    public Map<String, Object> getSummary() {
        return summaryRef.get();
    }

    public Map<String, Object> getByType() {
        return byTypeRef.get();
    }

    public Map<String, Object> getByManufacturer() {
        return byManufacturerRef.get();
    }

    private static Map<String, Object> defaultSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timestamp", Instant.now().toString());
        summary.put("totalUniqueDevices", 0L);
        summary.put("onlineCount", 0L);
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
