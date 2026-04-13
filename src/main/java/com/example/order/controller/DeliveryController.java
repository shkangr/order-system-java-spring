package com.example.order.controller;

import com.example.order.dto.DeliveryResponse;
import com.example.order.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Deliveries", description = "Delivery management API")
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @Operation(summary = "Get delivery info")
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(deliveryService.findDelivery(deliveryId));
    }

    @Operation(summary = "Start shipping", description = "READY → SHIPPING")
    @PostMapping("/{deliveryId}/ship")
    public ResponseEntity<Void> startShipping(@PathVariable Long deliveryId) {
        deliveryService.startShipping(deliveryId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Complete delivery", description = "SHIPPING → COMPLETED")
    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<Void> completeDelivery(@PathVariable Long deliveryId) {
        deliveryService.completeDelivery(deliveryId);
        return ResponseEntity.ok().build();
    }
}
