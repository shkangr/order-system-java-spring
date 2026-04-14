package com.example.order.service;

import com.example.order.config.annotation.Auditable;
import com.example.order.domain.*;
import com.example.order.dto.*;
import com.example.order.exception.EntityNotFoundException;
import com.example.order.repository.CouponRepository;
import com.example.order.repository.MemberCouponRepository;
import com.example.order.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;

    /**
     * Create a new coupon policy.
     */
    @Transactional
    public Long createCoupon(CreateCouponRequest request) {
        Coupon coupon = Coupon.createCoupon(
                request.getName(),
                DiscountType.valueOf(request.getDiscountType()),
                request.getDiscountValue(),
                request.getTotalQuantity(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getMinOrderAmount()
        );
        couponRepository.save(coupon);
        return coupon.getId();
    }

    /**
     * Update coupon info (protected by Optimistic Lock).
     * If two admins edit the same coupon simultaneously,
     * the second one gets ObjectOptimisticLockingFailureException.
     */
    @Auditable(action = "UPDATE_COUPON")
    @Transactional
    public void updateCoupon(Long couponId, UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found. id=" + couponId));

        coupon.updateInfo(request.getName(), request.getDiscountValue(), request.getMinOrderAmount());
        // @Version triggers optimistic lock check on flush
    }

    /**
     * Issue coupon to a member.
     * Uses Pessimistic Lock to prevent over-issuance in high-concurrency scenarios
     * (e.g., flash sale: 100 coupons, 1000 concurrent requests).
     */
    @Auditable(action = "ISSUE_COUPON")
    @Transactional
    public Long issueCoupon(Long couponId, Long memberId) {
        // Pessimistic Lock: SELECT ... FOR UPDATE on coupon row
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found. id=" + couponId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found. id=" + memberId));

        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new IllegalStateException("Member already has this coupon.");
        }

        coupon.issue(); // check quantity + increment issuedCount

        MemberCoupon memberCoupon = MemberCoupon.issue(member, coupon);
        memberCouponRepository.save(memberCoupon);

        return memberCoupon.getId();
    }

    /**
     * Use coupon for an order.
     * Called by OrderService during order creation.
     * Returns the discount amount calculated by the coupon.
     */
    @Transactional
    public int useCoupon(Long memberCouponId, Long orderId, int orderPrice) {
        // Pessimistic Lock: prevents concurrent use of the same coupon
        MemberCoupon memberCoupon = memberCouponRepository.findByIdWithLock(memberCouponId)
                .orElseThrow(() -> new EntityNotFoundException("MemberCoupon not found. id=" + memberCouponId));

        int discountAmount = memberCoupon.getCoupon().calculateDiscount(orderPrice);
        memberCoupon.use(orderId);

        return discountAmount;
    }

    /**
     * Soft delete a coupon.
     * The coupon becomes invisible in queries due to @SQLRestriction("deleted = false").
     */
    @Auditable(action = "DELETE_COUPON")
    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found. id=" + couponId));
        coupon.softDelete();
    }

    // === Query Methods === //

    public CouponResponse findCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found. id=" + couponId));
        return new CouponResponse(coupon);
    }

    /**
     * Dynamic search with QueryDSL Projection.
     * Returns lightweight DTOs directly from the query.
     */
    public Page<CouponSummaryDto> searchCoupons(CouponSearchCondition condition, Pageable pageable) {
        return couponRepository.searchCoupons(condition, pageable);
    }

    /**
     * Find member's coupons (all or by status).
     */
    public List<MemberCouponResponse> findMemberCoupons(Long memberId, CouponStatus status) {
        List<MemberCoupon> memberCoupons = (status != null)
                ? memberCouponRepository.findAllByMemberIdAndStatus(memberId, status)
                : memberCouponRepository.findAllByMemberIdWithCoupon(memberId);

        return memberCoupons.stream()
                .map(MemberCouponResponse::new)
                .toList();
    }
}
