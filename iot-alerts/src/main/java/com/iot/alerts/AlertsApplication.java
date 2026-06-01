package com.iot.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlertsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertsApplication.class, args);
    }
}

