package com.example.order.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level validation: ensures startDate field is before endDate field.
 *
 * Usage: @ValidDateRange(start = "validFrom", end = "validTo")
 *        public class CreateCouponRequest { ... }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDateRangeValidator.class)
public @interface ValidDateRange {

    String start();

    String end();

    String message() default "Start date must be before end date.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
