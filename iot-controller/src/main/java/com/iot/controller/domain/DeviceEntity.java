package com.iot.controller.domain;

import com.iot.shared.domain.components.Location;
import com.iot.shared.domain.components.Status;
import com.iot.shared.domain.components.Type;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Document(collection = "${app.mongodb.collection.devices}")
public record DeviceEntity(
                @Id String id,
                @NotNull Long deviceId,
                String name,
                String manufacturer,
                Type type,
                List<String> capabilities,
                Location location,
                Status status,
                Instant timestamp) {
}
