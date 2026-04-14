package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // 'order' is a SQL reserved word
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderDate;

    private Long memberCouponId;

    private int discountAmount;

    // === Bidirectional Convenience Methods === //

    public void setMember(Member member) {
        if (this.member != null) {
            this.member.getOrders().remove(this);
        }
        this.member = member;
        if (member != null) {
            member.getOrders().add(this);
        }
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        if (delivery != null) {
            delivery.assignOrder(this);
        }
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
        if (payment != null) {
            payment.assignOrder(this);
        }
    }

    // === Factory Method === //

    public static Order createOrder(Member member, List<OrderItem> orderItems,
                                    Delivery delivery, Payment payment) {
        return createOrder(member, orderItems, delivery, payment, null, 0);
    }

    public static Order createOrder(Member member, List<OrderItem> orderItems,
                                    Delivery delivery, Payment payment,
                                    Long memberCouponId, int discountAmount) {
        Order order = new Order();
        order.setMember(member);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        order.setDelivery(delivery);
        order.setPayment(payment);
        order.memberCouponId = memberCouponId;
        order.discountAmount = discountAmount;
        order.status = OrderStatus.ORDER;
        order.orderDate = LocalDateTime.now();
        return order;
    }

    // === Business Logic === //

    /**
     * Cancel order - restores stock, cancels delivery and payment
     * Cannot cancel if delivery is already shipping or completed
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCEL) {
            throw new IllegalStateException("Order is already cancelled.");
        }
        if (this.delivery != null &&
                (this.delivery.getStatus() == DeliveryStatus.SHIPPING ||
                 this.delivery.getStatus() == DeliveryStatus.COMPLETED)) {
            throw new IllegalStateException("Cannot cancel order. Delivery is already " + this.delivery.getStatus() + ".");
        }

        this.status = OrderStatus.CANCEL;
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
        }
        if (this.delivery != null && this.delivery.getStatus() == DeliveryStatus.READY) {
            this.delivery.cancel();
        }
        if (this.payment != null && this.payment.getStatus() == PaymentStatus.PAID) {
            this.payment.cancel();
        }
    }

    // === Query Logic === //

    /**
     * Calculate total price
     */
    public int getTotalPrice() {
        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }
}
