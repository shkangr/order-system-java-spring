package com.example.order.dto;

import com.example.order.domain.Delivery;
import com.example.order.domain.DeliveryStatus;
import lombok.Getter;

@Getter
public class DeliveryResponse {

    private final Long deliveryId;
    private final String receiverName;
    private final String phone;
    private final String zipCode;
    private final String address1;
    private final String address2;
    private final DeliveryStatus status;

    public DeliveryResponse(Delivery delivery) {
        this.deliveryId = delivery.getId();
        this.receiverName = delivery.getReceiverName();
        this.phone = delivery.getPhone();
        this.zipCode = delivery.getAddress().getZipCode();
        this.address1 = delivery.getAddress().getAddress1();
        this.address2 = delivery.getAddress().getAddress2();
        this.status = delivery.getStatus();
    }
}
