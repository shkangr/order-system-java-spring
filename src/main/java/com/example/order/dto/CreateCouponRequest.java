package com.example.order.dto;

import com.example.order.config.validation.ValidDateRange;
import com.example.order.config.validation.ValidEnum;
import com.example.order.domain.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@ValidDateRange(start = "validFrom", end = "validTo")
public class CreateCouponRequest {

    @NotBlank(message = "Coupon name is required.")
    private String name;

    @NotNull(message = "Discount type is required.")
    @ValidEnum(enumClass = DiscountType.class)
    private String discountType;

    @Min(value = 1, message = "Discount value must be at least 1.")
    private int discountValue;

    @Min(value = 1, message = "Total quantity must be at least 1.")
    private int totalQuantity;

    @NotNull(message = "Valid from date is required.")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid to date is required.")
    private LocalDateTime validTo;

    @Min(value = 0, message = "Minimum order amount cannot be negative.")
    private int minOrderAmount;
}
