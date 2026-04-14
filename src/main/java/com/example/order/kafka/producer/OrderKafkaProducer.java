package com.example.order.kafka.producer;

import com.example.order.event.OrderCancelledEvent;
import com.example.order.event.OrderCreatedEvent;
import com.example.order.kafka.event.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for Spring Application Events and produces Kafka messages.
 *
 * Uses AFTER_COMMIT phase — only sends to Kafka after the DB transaction succeeds.
 * This prevents sending notifications for transactions that may roll back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, OrderEventMessage> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderEventMessage message = OrderEventMessage.created(
                event.getOrderId(), event.getMemberName(),
                event.getTotalPrice(), event.getDiscountAmount()
        );

        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to send ORDER_CREATED. orderId={}", event.getOrderId(), ex);
                    } else {
                        log.info("[Kafka] Sent ORDER_CREATED. orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        OrderEventMessage message = OrderEventMessage.cancelled(
                event.getOrderId(), event.getMemberName()
        );

        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to send ORDER_CANCELLED. orderId={}", event.getOrderId(), ex);
                    } else {
                        log.info("[Kafka] Sent ORDER_CANCELLED. orderId={}, partition={}, offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
