package com.example.order.kafka.consumer;

import com.example.order.kafka.event.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that processes order events for notifications.
 *
 * Replaces the old direct SlackNotificationService call.
 * In production, this would call the actual Slack/Email/Push API.
 */
@Slf4j
@Component
public class OrderNotificationConsumer {

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void handleOrderEvent(OrderEventMessage message) {
        log.info("[Kafka Consumer] Received event: type={}, orderId={}",
                message.getEventType(), message.getOrderId());

        switch (message.getEventType()) {
            case "ORDER_CREATED" -> sendOrderCreatedNotification(message);
            case "ORDER_CANCELLED" -> sendOrderCancelledNotification(message);
            default -> log.warn("[Kafka Consumer] Unknown event type: {}", message.getEventType());
        }
    }

    private void sendOrderCreatedNotification(OrderEventMessage message) {
        String notification = String.format(
                ":shopping_cart: New order placed!\n" +
                "  Order ID: %d\n" +
                "  Customer: %s\n" +
                "  Total: %,d\n" +
                "  Discount: %,d",
                message.getOrderId(),
                message.getMemberName(),
                message.getTotalPrice(),
                message.getDiscountAmount()
        );
        log.info("[Slack] {}", notification);
    }

    private void sendOrderCancelledNotification(OrderEventMessage message) {
        String notification = String.format(
                ":x: Order cancelled!\n" +
                "  Order ID: %d\n" +
                "  Customer: %s",
                message.getOrderId(),
                message.getMemberName()
        );
        log.info("[Slack] {}", notification);
    }
}
