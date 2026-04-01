package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create order
     */
    @PostMapping
    public ResponseEntity<Long> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long orderId = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + orderId)).body(orderId);
    }

    /**
     * Cancel order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get single order
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.findOrder(orderId));
    }

    /**
     * Get all orders
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.findAllOrders());
    }

    /**
     * Get orders by member
     */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(orderService.findOrdersByMemberId(memberId));
    }
}
