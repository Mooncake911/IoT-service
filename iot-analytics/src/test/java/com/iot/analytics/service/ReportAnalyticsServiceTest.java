package com.iot.analytics.service;

import com.iot.analytics.domain.AnalyticsEntity;
import com.iot.analytics.repository.AnalyticsDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportAnalyticsService Tests")
class ReportAnalyticsServiceTest {

    @Mock
    private AnalyticsDataRepository repository;

    private static AnalyticsEntity row(Map<String, Object> metrics) {
        return new AnalyticsEntity("id", Instant.now(), metrics);
    }

    private static Map<String, Object> metrics(double total, double online, double battery, double signal) {
        Map<String, Object> m = new HashMap<>();
        m.put("totalDevices", total);
        m.put("onlineDevices", online);
        m.put("avgBatteryLevel", battery);
        m.put("avgSignalStrength", signal);
        return m;
    }

    @Test
    @DisplayName("empty rows produce zeroed report with window bounds")
    void windowReport_shouldHandleEmpty() {
        ReportAnalyticsService service = new ReportAnalyticsService(repository);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T01:00:00Z");
        when(repository.findAllByTimestampBetweenOrderByTimestampAsc(from, to)).thenReturn(Flux.empty());

        StepVerifier.create(service.windowReport(from, to))
                .assertNext(report -> {
                    assertThat(report.get("windows")).isEqualTo(0L);
                    assertThat(report.get("totalDevicesProcessed")).isEqualTo(0.0);
                    assertThat(report.get("onlineRate")).isEqualTo(0.0);
                    assertThat(report.get("avgBatteryLevel")).isEqualTo(0.0);
                    assertThat(report.get("avgSignalStrength")).isEqualTo(0.0);
                    assertThat(report.get("from")).isEqualTo(from.toString());
                    assertThat(report.get("to")).isEqualTo(to.toString());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("rows are aggregated with device-weighted averages")
    void windowReport_shouldAggregateWeighted() {
        ReportAnalyticsService service = new ReportAnalyticsService(repository);
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        when(repository.findAllByTimestampBetweenOrderByTimestampAsc(any(), any())).thenReturn(Flux.just(
                row(metrics(10, 8, 80, 70)),
                row(metrics(30, 15, 50, 60))));

        StepVerifier.create(service.windowReport(from, to))
                .assertNext(report -> {
                    assertThat(report.get("windows")).isEqualTo(2L);
                    assertThat(report.get("totalDevicesProcessed")).isEqualTo(40.0);
                    assertThat(report).containsEntry("onlineRate", 23.0 / 40.0);
                    assertThat(report.get("avgBatteryLevel")).isEqualTo((800.0 + 1500.0) / 40.0);
                    assertThat(report.get("avgSignalStrength")).isEqualTo((700.0 + 1800.0) / 40.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("rows with null, empty or zero-total metrics are skipped")
    void windowReport_shouldSkipBadRows() {
        ReportAnalyticsService service = new ReportAnalyticsService(repository);
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        Map<String, Object> zeroTotal = metrics(0, 0, 99, 99);
        Map<String, Object> strings = new HashMap<>();
        strings.put("totalDevices", "ten");
        strings.put("onlineDevices", null);
        strings.put("avgBatteryLevel", 50);
        strings.put("avgSignalStrength", 50);
        when(repository.findAllByTimestampBetweenOrderByTimestampAsc(any(), any())).thenReturn(Flux.just(
                row(null),
                row(Map.of()),
                row(zeroTotal),
                row(strings),
                row(metrics(4, 2, 50, 50))));

        StepVerifier.create(service.windowReport(from, to))
                .assertNext(report -> {
                    assertThat(report.get("windows")).isEqualTo(5L);
                    assertThat(report).containsEntry("totalDevicesProcessed", 4.0);
                    assertThat(report).containsEntry("onlineRate", 0.5);
                })
                .verifyComplete();
    }
}
