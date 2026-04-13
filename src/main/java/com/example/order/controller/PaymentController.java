package com.example.order.controller;

import com.example.order.dto.PaymentResponse;
import com.example.order.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "Payment management API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Get payment info")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.findPayment(paymentId));
    }

    @Operation(summary = "Approve payment", description = "READY → PAID")
    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<Void> approvePayment(@PathVariable Long paymentId) {
        paymentService.approvePayment(paymentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Fail payment", description = "READY → FAILED")
    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<Void> failPayment(@PathVariable Long paymentId) {
        paymentService.failPayment(paymentId);
        return ResponseEntity.ok().build();
    }
}
