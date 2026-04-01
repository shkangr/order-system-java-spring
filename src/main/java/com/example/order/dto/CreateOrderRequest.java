package com.example.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Member ID is required.")
    private Long memberId;

    @Valid
    @Size(min = 1, message = "At least 1 order item is required.")
    private List<OrderItemRequest> orderItems;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required.")
        private Long productId;

        @Min(value = 1, message = "Order quantity must be at least 1.")
        private int count;
    }
}
