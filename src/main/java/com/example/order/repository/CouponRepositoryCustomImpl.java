package com.example.order.repository;

import com.example.order.domain.DiscountType;
import com.example.order.dto.CouponSearchCondition;
import com.example.order.dto.CouponSummaryDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.order.domain.QCoupon.coupon;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryCustomImpl implements CouponRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CouponSummaryDto> searchCoupons(CouponSearchCondition condition, Pageable pageable) {
        List<CouponSummaryDto> content = queryFactory
                .select(Projections.constructor(CouponSummaryDto.class,
                        coupon.id,
                        coupon.name,
                        coupon.discountType,
                        coupon.discountValue,
                        coupon.totalQuantity,
                        coupon.issuedCount,
                        coupon.validFrom,
                        coupon.validTo
                ))
                .from(coupon)
                .where(
                        nameContains(condition.getName()),
                        discountTypeEq(condition.getDiscountType()),
                        isActive(condition.getActive()),
                        validFromAfter(condition.getFrom()),
                        validToBefore(condition.getTo())
                )
                .orderBy(coupon.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(coupon.count())
                .from(coupon)
                .where(
                        nameContains(condition.getName()),
                        discountTypeEq(condition.getDiscountType()),
                        isActive(condition.getActive()),
                        validFromAfter(condition.getFrom()),
                        validToBefore(condition.getTo())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // === Dynamic Where Conditions === //

    private BooleanExpression nameContains(String name) {
        return name != null ? coupon.name.containsIgnoreCase(name) : null;
    }

    private BooleanExpression discountTypeEq(DiscountType discountType) {
        return discountType != null ? coupon.discountType.eq(discountType) : null;
    }

    private BooleanExpression isActive(Boolean active) {
        if (active == null) return null;
        LocalDateTime now = LocalDateTime.now();
        return active
                ? coupon.validFrom.loe(now).and(coupon.validTo.goe(now))
                : coupon.validTo.lt(now).or(coupon.validFrom.gt(now));
    }

    private BooleanExpression validFromAfter(LocalDateTime from) {
        return from != null ? coupon.validFrom.goe(from) : null;
    }

    private BooleanExpression validToBefore(LocalDateTime to) {
        return to != null ? coupon.validTo.loe(to) : null;
    }
}
