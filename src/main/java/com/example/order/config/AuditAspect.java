package com.example.order.config;

import com.example.order.config.annotation.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect for audit logging on methods annotated with @Auditable.
 *
 * Pointcut: annotation-based — only applies to methods with @Auditable.
 * Unlike PerformanceAspect which targets all service methods,
 * this selectively tracks important business state changes.
 *
 * Uses two advice types:
 *   - @AfterReturning: logs on success
 *   - @AfterThrowing:  logs on failure (does NOT swallow the exception)
 */
@Slf4j
@Aspect
@Component
public class AuditAspect {

    /**
     * Triggered after a method with @Auditable completes successfully.
     *
     * @param joinPoint provides method info (class, name, arguments)
     * @param auditable the annotation instance — gives access to action() value
     *                  (bound via "@annotation(auditable)" pointcut expression)
     */
    @AfterReturning("@annotation(auditable)")
    public void logAuditSuccess(JoinPoint joinPoint, Auditable auditable) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = Arrays.toString(joinPoint.getArgs());
        String action = auditable.action().isEmpty() ? methodName : auditable.action();

        log.info("[Audit] action={}, method={}.{}(), args={}, thread={}",
                action, className, methodName, args, Thread.currentThread().getName());
    }

    /**
     * Triggered after a method with @Auditable throws an exception.
     * The exception is NOT caught — it continues to propagate to GlobalExceptionHandler.
     *
     * @param ex the thrown exception (bound via throwing = "ex" parameter)
     */
    @AfterThrowing(value = "@annotation(auditable)", throwing = "ex")
    public void logAuditFailure(JoinPoint joinPoint, Auditable auditable, Exception ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = Arrays.toString(joinPoint.getArgs());
        String action = auditable.action().isEmpty() ? methodName : auditable.action();

        log.warn("[Audit FAILED] action={}, method={}.{}(), args={}, error={}",
                action, className, methodName, args, ex.getMessage());
    }
}
