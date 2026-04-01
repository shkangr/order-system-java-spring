package com.example.order.controller;

import com.example.order.dto.DeliveryResponse;
import com.example.order.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * Get delivery info
     */
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(deliveryService.findDelivery(deliveryId));
    }

    /**
     * Start shipping
     */
    @PostMapping("/{deliveryId}/ship")
    public ResponseEntity<Void> startShipping(@PathVariable Long deliveryId) {
        deliveryService.startShipping(deliveryId);
        return ResponseEntity.ok().build();
    }

    /**
     * Complete delivery
     */
    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<Void> completeDelivery(@PathVariable Long deliveryId) {
        deliveryService.completeDelivery(deliveryId);
        return ResponseEntity.ok().build();
    }
}
