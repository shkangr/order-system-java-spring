package com.example.order.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> allowedValues;
    private String enumName;

    @Override
    public void initialize(ValidEnum annotation) {
        enumName = annotation.enumClass().getSimpleName();
        allowedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null check is @NotNull's responsibility
        }

        boolean valid = allowedValues.contains(value);
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid value '" + value + "'. Allowed values: " + allowedValues
            ).addConstraintViolation();
        }
        return valid;
    }
}
