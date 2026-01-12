package com.iot.controller.controller;

import com.iot.controller.service.IngestionService;
import com.iot.shared.domain.DeviceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/ingest")
@Slf4j
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> ingestBatch(@RequestBody List<DeviceData> deviceDataList) {
        log.info("Received ingestion batch: size={}", deviceDataList.size());
        return ingestionService.ingestBatch(deviceDataList);
    }
}
