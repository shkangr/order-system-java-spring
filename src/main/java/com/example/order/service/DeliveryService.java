package com.example.order.service;

import com.example.order.config.annotation.Auditable;
import com.example.order.domain.Delivery;
import com.example.order.dto.DeliveryResponse;
import com.example.order.exception.EntityNotFoundException;
import com.example.order.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Auditable(action = "START_SHIPPING")
    @Transactional
    public void startShipping(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found. id=" + deliveryId));
        delivery.startShipping();
    }

    @Auditable(action = "COMPLETE_DELIVERY")
    @Transactional
    public void completeDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found. id=" + deliveryId));
        delivery.complete();
    }

    public DeliveryResponse findDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found. id=" + deliveryId));
        return new DeliveryResponse(delivery);
    }
}
