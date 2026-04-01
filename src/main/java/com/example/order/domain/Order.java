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
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime orderDate;

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

    // === Factory Method === //

    public static Order createOrder(Member member, List<OrderItem> orderItems) {
        Order order = new Order();
        order.setMember(member);
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        order.status = OrderStatus.ORDER;
        order.orderDate = LocalDateTime.now();
        return order;
    }

    // === Business Logic === //

    /**
     * Cancel order - restores stock
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCEL) {
            throw new IllegalStateException("Order is already cancelled.");
        }
        this.status = OrderStatus.CANCEL;
        for (OrderItem orderItem : orderItems) {
            orderItem.cancel();
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
