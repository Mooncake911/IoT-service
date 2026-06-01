package com.iot.gateway.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ApiDocsController {

    @Value("${ANALYTICS_URL:http://localhost:8083}")
    private String analyticsUrl;

    @Value("${ALERTS_URL:http://localhost:8084}")
    private String alertsUrl;

    @Value("${SIMULATOR_URL:http://localhost:8081}")
    private String simulatorUrl;

    @Value("${CONTROLLER_URL:http://localhost:8082}")
    private String controllerUrl;

    @GetMapping({"/api/docs", "/api/v1/docs"})
    public Map<String, Object> docs() {
        Map<String, String> services = new LinkedHashMap<>();
        services.put("gateway", "/v3/api-docs");
        services.put("analytics", analyticsUrl + "/v3/api-docs");
        services.put("alerts", alertsUrl + "/v3/api-docs");
        services.put("simulator", simulatorUrl + "/v3/api-docs");
        services.put("controller", controllerUrl + "/v3/api-docs");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "iot-data-gateway");
        response.put("apiVersion", "v1");
        response.put("docs", services);
        return response;
    }
}
