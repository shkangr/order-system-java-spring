package com.example.order.dto;

import com.example.order.domain.Coupon;
import com.example.order.domain.DiscountType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponResponse {

    private final Long id;
    private final String name;
    private final DiscountType discountType;
    private final int discountValue;
    private final int totalQuantity;
    private final int issuedCount;
    private final int remainingQuantity;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final int minOrderAmount;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CouponResponse(Coupon coupon) {
        this.id = coupon.getId();
        this.name = coupon.getName();
        this.discountType = coupon.getDiscountType();
        this.discountValue = coupon.getDiscountValue();
        this.totalQuantity = coupon.getTotalQuantity();
        this.issuedCount = coupon.getIssuedCount();
        this.remainingQuantity = coupon.getTotalQuantity() - coupon.getIssuedCount();
        this.validFrom = coupon.getValidFrom();
        this.validTo = coupon.getValidTo();
        this.minOrderAmount = coupon.getMinOrderAmount();
        this.active = coupon.isValid();
        this.createdAt = coupon.getCreatedAt();
        this.updatedAt = coupon.getUpdatedAt();
    }
}
