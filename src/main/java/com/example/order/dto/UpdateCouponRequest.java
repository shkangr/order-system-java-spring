package com.example.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateCouponRequest {

    @NotBlank(message = "Coupon name is required.")
    private String name;

    @Min(value = 1, message = "Discount value must be at least 1.")
    private int discountValue;

    @Min(value = 0, message = "Minimum order amount cannot be negative.")
    private int minOrderAmount;
}
