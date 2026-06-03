package com.iot.analytics.service;

import com.iot.analytics.domain.AnalyticsEntity;
import com.iot.analytics.repository.AnalyticsDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportAnalyticsService {

    private final AnalyticsDataRepository analyticsDataRepository;

    public Mono<Map<String, Object>> windowReport(Instant from, Instant to) {
        return analyticsDataRepository.findAllByTimestampBetweenOrderByTimestampAsc(from, to)
                .collectList()
                .map(rows -> aggregate(rows, from, to));
    }

    private Map<String, Object> aggregate(List<AnalyticsEntity> rows, Instant from, Instant to) {
        long windows = rows.size();
        double weightedTotalDevices = 0.0;
        double weightedOnlineDevices = 0.0;
        double weightedBattery = 0.0;
        double weightedSignal = 0.0;

        for (AnalyticsEntity row : rows) {
            Map<String, Object> m = row.metrics();
            if (m == null || m.isEmpty()) {
                continue;
            }
            double total = asDouble(m.get("totalDevices"));
            if (total <= 0) {
                continue;
            }
            double online = asDouble(m.get("onlineDevices"));
            double avgBattery = asDouble(m.get("avgBatteryLevel"));
            double avgSignal = asDouble(m.get("avgSignalStrength"));

            weightedTotalDevices += total;
            weightedOnlineDevices += online;
            weightedBattery += avgBattery * total;
            weightedSignal += avgSignal * total;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("windows", windows);
        out.put("totalDevicesProcessed", weightedTotalDevices);
        out.put("onlineRate", weightedTotalDevices == 0 ? 0.0 : weightedOnlineDevices / weightedTotalDevices);
        out.put("avgBatteryLevel", weightedTotalDevices == 0 ? 0.0 : weightedBattery / weightedTotalDevices);
        out.put("avgSignalStrength", weightedTotalDevices == 0 ? 0.0 : weightedSignal / weightedTotalDevices);
        return out;
    }

    private static double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }
}
