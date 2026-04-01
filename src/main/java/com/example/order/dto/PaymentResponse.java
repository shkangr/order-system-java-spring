package com.example.order.dto;

import com.example.order.domain.Payment;
import com.example.order.domain.PaymentMethod;
import com.example.order.domain.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentResponse {

    private final Long paymentId;
    private final String paymentKey;
    private final PaymentMethod method;
    private final int amount;
    private final PaymentStatus status;
    private final LocalDateTime approvedAt;

    public PaymentResponse(Payment payment) {
        this.paymentId = payment.getId();
        this.paymentKey = payment.getPaymentKey();
        this.method = payment.getMethod();
        this.amount = payment.getAmount();
        this.status = payment.getStatus();
        this.approvedAt = payment.getApprovedAt();
    }
}
