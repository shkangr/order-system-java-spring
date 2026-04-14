package com.example.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spring Application Event — published when an order is successfully created.
 * Consumed by Kafka producer to send notifications asynchronously.
 */
@Getter
@AllArgsConstructor
public class OrderCreatedEvent {

    private final Long orderId;
    private final String memberName;
    private final int totalPrice;
    private final int discountAmount;
}
