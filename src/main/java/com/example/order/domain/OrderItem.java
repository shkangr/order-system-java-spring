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

    // === 패키지 내부에서만 사용 (Order.addOrderItem에서 호출) === //
    void assignOrder(Order order) {
        this.order = order;
    }

    // === 생성 메서드 === //

    /**
     * 주문 아이템 생성 + 재고 차감
     */
    public static OrderItem createOrderItem(Product product, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.product = product;
        orderItem.orderPrice = orderPrice;
        orderItem.count = count;

        // 재고 차감
        product.removeStock(count);
        return orderItem;
    }

    // === 비즈니스 로직 === //

    /**
     * 주문 취소 시 재고 복구
     */
    public void cancel() {
        product.addStock(count);
    }

    /**
     * 주문 아이템 총액
     */
    public int getTotalPrice() {
        return orderPrice * count;
    }
}
