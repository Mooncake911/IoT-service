package com.iot.controller.validation;

import com.iot.contracts.domain.DeviceData;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DeviceValidator {

    private final Validator validator;

    public void validate(DeviceData deviceData) {
        Set<ConstraintViolation<DeviceData>> violations = validator.validate(deviceData);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Device validation failed", violations);
        }
    }
}
