package com.example.order.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a String field contains a valid enum constant name.
 *
 * Usage: @ValidEnum(enumClass = PaymentMethod.class)
 *        private String paymentMethod;
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnumValidator.class)
public @interface ValidEnum {

    Class<? extends Enum<?>> enumClass();

    String message() default "Invalid value. Allowed: {enumClass}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
