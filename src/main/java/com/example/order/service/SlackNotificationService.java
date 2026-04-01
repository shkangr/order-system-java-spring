package com.example.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SlackNotificationService {

    @Async("notificationExecutor")
    public void sendOrderCreatedMessage(Long orderId, String memberName, int totalPrice) {
        log.info("[Slack] Sending order notification - thread={}", Thread.currentThread().getName());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String message = String.format(
                ":shopping_cart: New order placed!\n" +
                "• Order ID: %d\n" +
                "• Customer: %s\n" +
                "• Total: %,d",
                orderId, memberName, totalPrice
        );

        log.info("[Slack] Message sent - {}", message);
    }

    @Async("notificationExecutor")
    public void sendOrderCancelledMessage(Long orderId, String memberName) {
        log.info("[Slack] Sending cancellation notification - thread={}", Thread.currentThread().getName());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String message = String.format(
                ":x: Order cancelled!\n" +
                "• Order ID: %d\n" +
                "• Customer: %s",
                orderId, memberName
        );

        log.info("[Slack] Message sent - {}", message);
    }
}
