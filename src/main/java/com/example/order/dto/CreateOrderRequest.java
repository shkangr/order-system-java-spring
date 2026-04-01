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

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId;

    @Valid
    @Size(min = 1, message = "최소 1개 이상의 주문 아이템이 필요합니다.")
    private List<OrderItemRequest> orderItems;

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @Min(value = 1, message = "주문 수량은 최소 1개 이상이어야 합니다.")
        private int count;
    }
}
