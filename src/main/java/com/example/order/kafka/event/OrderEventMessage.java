package com.example.order.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka message payload for order events.
 * Serialized as JSON and published to "order-events" topic.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage {

    private String eventType;       // ORDER_CREATED, ORDER_CANCELLED
    private Long orderId;
    private String memberName;
    private Integer totalPrice;
    private Integer discountAmount;
    private LocalDateTime timestamp;

    public static OrderEventMessage created(Long orderId, String memberName,
                                            int totalPrice, int discountAmount) {
        return new OrderEventMessage(
                "ORDER_CREATED", orderId, memberName, totalPrice, discountAmount, LocalDateTime.now()
        );
    }

    public static OrderEventMessage cancelled(Long orderId, String memberName) {
        return new OrderEventMessage(
                "ORDER_CANCELLED", orderId, memberName, null, null, LocalDateTime.now()
        );
    }
}
