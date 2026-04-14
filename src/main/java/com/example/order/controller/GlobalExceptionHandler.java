package com.example.order.controller;

import com.example.order.exception.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Entity not found (Member, Product, Order)
     * -> 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("[EntityNotFound] {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Invalid request parameter
     * -> 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[IllegalArgument] {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Business rule violation (not enough stock, already cancelled)
     * -> 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("[IllegalState] {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Optimistic Lock conflict (concurrent coupon update)
     * -> 409 Conflict with retry hint
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("[OptimisticLock] {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Data was modified by another user. Please retry.");
    }

    /**
     * @Valid validation failure
     * -> 400 Bad Request with field-level error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");

        log.warn("[Validation] {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Unknown API path
     * -> 404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException e) {
        log.warn("[NotFound] {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Unexpected server error
     * -> 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("[Unhandled Exception]", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
