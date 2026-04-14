package com.example.order.dto;

import com.example.order.domain.CouponStatus;
import com.example.order.domain.DiscountType;
import com.example.order.domain.MemberCoupon;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberCouponResponse {

    private final Long memberCouponId;
    private final Long couponId;
    private final String couponName;
    private final DiscountType discountType;
    private final int discountValue;
    private final CouponStatus status;
    private final Long usedOrderId;
    private final LocalDateTime usedAt;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final LocalDateTime issuedAt;

    public MemberCouponResponse(MemberCoupon memberCoupon) {
        this.memberCouponId = memberCoupon.getId();
        this.couponId = memberCoupon.getCoupon().getId();
        this.couponName = memberCoupon.getCoupon().getName();
        this.discountType = memberCoupon.getCoupon().getDiscountType();
        this.discountValue = memberCoupon.getCoupon().getDiscountValue();
        this.status = memberCoupon.getStatus();
        this.usedOrderId = memberCoupon.getUsedOrderId();
        this.usedAt = memberCoupon.getUsedAt();
        this.validFrom = memberCoupon.getCoupon().getValidFrom();
        this.validTo = memberCoupon.getCoupon().getValidTo();
        this.issuedAt = memberCoupon.getCreatedAt();
    }
}
