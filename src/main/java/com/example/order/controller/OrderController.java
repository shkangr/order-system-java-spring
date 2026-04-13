package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Orders", description = "Order management API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create order", description = "Creates order with delivery and payment, deducts stock")
    @PostMapping
    public ResponseEntity<Long> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long orderId = orderService.createOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + orderId)).body(orderId);
    }

    @Operation(summary = "Cancel order", description = "Cancels order, restores stock. Fails if delivery is already shipping")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get single order")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.findOrder(orderId));
    }

    @Operation(summary = "Get all orders (paginated)", description = "e.g. ?page=0&size=10")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(orderService.findOrdersPaged(pageable));
    }

    @Operation(summary = "Get orders by member")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(orderService.findOrdersByMemberId(memberId));
    }
}
