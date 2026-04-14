package com.example.order.repository;

import com.example.order.dto.CouponSearchCondition;
import com.example.order.dto.CouponSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponRepositoryCustom {

    /**
     * Dynamic search with QueryDSL Projection.
     * Returns lightweight DTOs instead of full entities.
     */
    Page<CouponSummaryDto> searchCoupons(CouponSearchCondition condition, Pageable pageable);
}
