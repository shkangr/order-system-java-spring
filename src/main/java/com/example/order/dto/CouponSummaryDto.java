package com.example.order.dto;

import com.example.order.domain.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for coupon list queries (QueryDSL Projection).
 * Only selected columns are fetched — no entity loaded into persistence context.
 */
@Getter
@AllArgsConstructor
public class CouponSummaryDto {

    private Long id;
    private String name;
    private DiscountType discountType;
    private int discountValue;
    private int totalQuantity;
    private int issuedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
