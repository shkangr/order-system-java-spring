package com.example.order.dto;

import com.example.order.domain.DiscountType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CouponSearchCondition {

    private String name;
    private DiscountType discountType;
    private Boolean active;  // true = currently valid coupons only
    private LocalDateTime from;
    private LocalDateTime to;
}
