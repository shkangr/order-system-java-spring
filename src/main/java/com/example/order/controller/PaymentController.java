package com.example.order.controller;

import com.example.order.dto.PaymentResponse;
import com.example.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Get payment info
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.findPayment(paymentId));
    }

    /**
     * Approve payment
     */
    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<Void> approvePayment(@PathVariable Long paymentId) {
        paymentService.approvePayment(paymentId);
        return ResponseEntity.ok().build();
    }

    /**
     * Fail payment
     */
    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<Void> failPayment(@PathVariable Long paymentId) {
        paymentService.failPayment(paymentId);
        return ResponseEntity.ok().build();
    }
}
