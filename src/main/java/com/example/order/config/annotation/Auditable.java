package com.example.order.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for audit logging via AOP.
 *
 * Place on service methods that perform important state changes
 * (e.g. order cancel, payment approve, delivery ship).
 * AuditAspect will automatically log the action, arguments, and result.
 *
 * Usage: @Auditable(action = "CANCEL_ORDER")
 */
@Target(ElementType.METHOD)          // Can only be placed on methods
@Retention(RetentionPolicy.RUNTIME)  // Available at runtime (required for AOP to detect it)
public @interface Auditable {

    /**
     * Describes the business action being performed.
     * If empty, the method name is used as the action.
     */
    String action() default "";
}
