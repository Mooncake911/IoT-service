package com.iot.controller.controller;

import com.iot.controller.service.IngestionService;
import com.iot.shared.domain.Device;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> ingestBatch(@RequestBody List<Device> deviceList) {
        return Flux.fromIterable(deviceList)
                .flatMap(ingestionService::ingestData)
                .then();
    }
}
