package com.example.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spring Application Event — published when an order is cancelled.
 *
 * Listeners:
 *   - CouponEventListener (BEFORE_COMMIT): restores coupon in the same transaction
 *   - KafkaEventListener (AFTER_COMMIT): sends notification via Kafka
 */
@Getter
@AllArgsConstructor
public class OrderCancelledEvent {

    private final Long orderId;
    private final String memberName;
    private final Long memberCouponId;
}
