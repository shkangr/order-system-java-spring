package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private Order order;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private int amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime approvedAt;

    // === Factory Method === //
    public static Payment createPayment(PaymentMethod method, int amount) {
        Payment payment = new Payment();
        payment.paymentKey = UUID.randomUUID().toString();
        payment.method = method;
        payment.amount = amount;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    // === Package-private: called by Order.setPayment() === //
    void assignOrder(Order order) {
        this.order = order;
    }

    // === Business Logic === //

    public void approve() {
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("Payment can only be approved from READY status.");
        }
        this.status = PaymentStatus.PAID;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("Payment can only fail from READY status.");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        if (this.status != PaymentStatus.PAID) {
            throw new IllegalStateException("Only paid payments can be canceled.");
        }
        this.status = PaymentStatus.CANCELED;
    }
}
