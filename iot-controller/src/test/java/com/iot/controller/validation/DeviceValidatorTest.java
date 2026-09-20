package com.iot.controller.validation;

import com.iot.contracts.domain.DeviceData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceValidator Tests")
class DeviceValidatorTest {

    @Mock
    private Validator validator;

    @Test
    @DisplayName("valid device passes without exception")
    void validate_shouldPassWhenNoViolations() {
        DeviceValidator deviceValidator = new DeviceValidator(validator);
        DeviceData device = DeviceData.builder().build();
        when(validator.validate(device)).thenReturn(Set.of());

        assertThatCode(() -> deviceValidator.validate(device)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("violations raise ConstraintViolationException")
    void validate_shouldThrowOnViolations() {
        DeviceValidator deviceValidator = new DeviceValidator(validator);
        DeviceData device = DeviceData.builder().build();
        @SuppressWarnings("unchecked")
        ConstraintViolation<DeviceData> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        when(validator.validate(device)).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> deviceValidator.validate(device))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("Device validation failed");
    }

    @Test
    @DisplayName("real validator rejects blank device name")
    void validate_shouldRejectBlankNameWithRealValidator() {
        DeviceValidator deviceValidator = new DeviceValidator(
                jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator());
        DeviceData device = DeviceData.builder().build();

        assertThatThrownBy(() -> deviceValidator.validate(device))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
}
