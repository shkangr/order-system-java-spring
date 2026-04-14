package com.example.order.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startFieldName;
    private String endFieldName;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.startFieldName = annotation.start();
        this.endFieldName = annotation.end();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field startField = value.getClass().getDeclaredField(startFieldName);
            Field endField = value.getClass().getDeclaredField(endFieldName);
            startField.setAccessible(true);
            endField.setAccessible(true);

            LocalDateTime start = (LocalDateTime) startField.get(value);
            LocalDateTime end = (LocalDateTime) endField.get(value);

            if (start == null || end == null) {
                return true; // null check is @NotNull's responsibility
            }

            return start.isBefore(end);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Invalid field name in @ValidDateRange", e);
        }
    }
}
