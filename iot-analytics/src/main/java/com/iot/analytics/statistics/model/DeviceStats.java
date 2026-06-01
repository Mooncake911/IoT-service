package com.iot.analytics.statistics.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class DeviceStats {
    @Builder.Default
    Map<String, Object> metrics = Map.of();
}
