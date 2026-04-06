package com.example.order.service;

import com.example.order.config.annotation.Auditable;
import com.example.order.domain.Payment;
import com.example.order.dto.PaymentResponse;
import com.example.order.exception.EntityNotFoundException;
import com.example.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Auditable(action = "APPROVE_PAYMENT")
    @Transactional
    public void approvePayment(Long paymentId) {
        // Pessimistic Lock: prevents concurrent approve/fail
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found. id=" + paymentId));
        payment.approve();
    }

    @Auditable(action = "FAIL_PAYMENT")
    @Transactional
    public void failPayment(Long paymentId) {
        // Pessimistic Lock: prevents concurrent approve/fail
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found. id=" + paymentId));
        payment.fail();
    }

    public PaymentResponse findPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found. id=" + paymentId));
        return new PaymentResponse(payment);
    }
}
