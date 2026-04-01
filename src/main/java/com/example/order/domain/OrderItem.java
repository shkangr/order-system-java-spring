package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int orderPrice;

    private int count;

    // === Package-private: called by Order.addOrderItem() === //
    void assignOrder(Order order) {
        this.order = order;
    }

    // === Factory Method === //

    /**
     * Create order item + deduct stock
     */
    public static OrderItem createOrderItem(Product product, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.product = product;
        orderItem.orderPrice = orderPrice;
        orderItem.count = count;

        product.removeStock(count);
        return orderItem;
    }

    // === Business Logic === //

    /**
     * Cancel - restore stock
     */
    public void cancel() {
        product.addStock(count);
    }

    /**
     * Order item total price
     */
    public int getTotalPrice() {
        return orderPrice * count;
    }
}
