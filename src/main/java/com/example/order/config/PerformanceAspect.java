package com.example.order.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for logging execution time of all service methods.
 *
 * Pointcut: package-based — automatically applies to every method
 * under com.example.order.service without any code changes in Service classes.
 *
 * Advice type: @Around — wraps the method execution to measure time before and after.
 */
@Slf4j
@Aspect     // Declares this class as an AOP Aspect
@Component  // Registers as a Spring Bean (required for @Aspect to work)
public class PerformanceAspect {

    /**
     * Pointcut: execution(* com.example.order.service..*(..))
     *   - *                    : any return type
     *   - service..            : service package and all sub-packages
     *   - *(..)               : any method name, any parameters
     *
     * @param joinPoint provides access to the intercepted method's info
     *                  (class name, method name, arguments, etc.)
     * @return the original method's return value, passed through unchanged
     */
    @Around("execution(* com.example.order.service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();
        try {
            // proceed() calls the actual target method
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[Performance] {}.{}() - {}ms", className, methodName, elapsed);
            return result;
        } catch (Throwable e) {
            // Log with WARN level on failure, then re-throw so the exception propagates normally
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[Performance] {}.{}() - {}ms (FAILED: {})", className, methodName, elapsed, e.getMessage());
            throw e;
        }
    }
}
