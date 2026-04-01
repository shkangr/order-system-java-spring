package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    private String name;

    private int price;

    private int stockQuantity;

    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();

    // === Factory Method === //
    public static Product createProduct(String name, int price, int stockQuantity) {
        Product product = new Product();
        product.name = name;
        product.price = price;
        product.stockQuantity = stockQuantity;
        return product;
    }

    // === Business Logic === //

    /**
     * Deduct stock
     */
    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalStateException("Not enough stock. Current stock: " + this.stockQuantity);
        }
        this.stockQuantity = restStock;
    }

    /**
     * Restore stock
     */
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
