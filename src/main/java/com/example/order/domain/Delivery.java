package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private Order order;

    private String receiverName;

    private String phone;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    // === Factory Method === //
    public static Delivery createDelivery(String receiverName, String phone, Address address) {
        Delivery delivery = new Delivery();
        delivery.receiverName = receiverName;
        delivery.phone = phone;
        delivery.address = address;
        delivery.status = DeliveryStatus.READY;
        return delivery;
    }

    // === Package-private: called by Order.setDelivery() === //
    void assignOrder(Order order) {
        this.order = order;
    }

    // === Business Logic === //

    public void startShipping() {
        if (this.status != DeliveryStatus.READY) {
            throw new IllegalStateException("Delivery can only start shipping from READY status.");
        }
        this.status = DeliveryStatus.SHIPPING;
    }

    public void complete() {
        if (this.status != DeliveryStatus.SHIPPING) {
            throw new IllegalStateException("Delivery can only be completed from SHIPPING status.");
        }
        this.status = DeliveryStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status != DeliveryStatus.READY) {
            throw new IllegalStateException("Delivery can only be canceled in READY status.");
        }
        this.status = DeliveryStatus.CANCELED;
    }
}
