package com.example.order.dto;

import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.domain.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {

    private final Long orderId;
    private final String memberName;
    private final OrderStatus status;
    private final LocalDateTime orderDate;
    private final int totalPrice;
    private final List<OrderItemResponse> orderItems;
    private final DeliveryResponse delivery;
    private final PaymentResponse payment;

    public OrderResponse(Order order) {
        this.orderId = order.getId();
        this.memberName = order.getMember().getName();
        this.status = order.getStatus();
        this.orderDate = order.getOrderDate();
        this.totalPrice = order.getTotalPrice();
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemResponse::new)
                .toList();
        this.delivery = order.getDelivery() != null ? new DeliveryResponse(order.getDelivery()) : null;
        this.payment = order.getPayment() != null ? new PaymentResponse(order.getPayment()) : null;
    }

    @Getter
    public static class OrderItemResponse {
        private final Long productId;
        private final String productName;
        private final int orderPrice;
        private final int count;
        private final int totalPrice;

        public OrderItemResponse(OrderItem orderItem) {
            this.productId = orderItem.getProduct().getId();
            this.productName = orderItem.getProduct().getName();
            this.orderPrice = orderItem.getOrderPrice();
            this.count = orderItem.getCount();
            this.totalPrice = orderItem.getTotalPrice();
        }
    }
}
