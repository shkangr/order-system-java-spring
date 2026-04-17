package com.example.order.service;

import com.example.order.config.DistributedLockExecutor;
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

import java.util.concurrent.TimeUnit;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final CouponRedisService couponRedisService;
    private final DistributedLockExecutor distributedLockExecutor;

    /**
     * Create a new coupon policy.
     * Also initializes coupon remaining count in Redis for fast pre-filtering.
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

        // Sync to Redis: remaining = totalQuantity (issuedCount is 0 for new coupon)
        couponRedisService.syncCoupon(coupon.getId(), coupon.getTotalQuantity(), 0);

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
     *
     * Three layers of protection:
     *   1. Redis DECR — fast pre-filter, rejects sold-out requests without touching DB
     *   2. Redisson Distributed Lock — serializes per couponId, prevents DB connection flood
     *   3. DB Pessimistic Lock — final source-of-truth guarantee
     *
     * For a flash sale (100 coupons, 10,000 concurrent requests):
     *   - Without Redis: 10,000 requests hit DB, 9,900 wait on row lock
     *   - With Redis: ~100 pass Redis filter → ~100 acquire distributed lock → DB confirms
     */
    @Auditable(action = "ISSUE_COUPON")
    @Transactional
    public Long issueCoupon(Long couponId, Long memberId) {
        // Layer 1: Redis atomic counter — reject sold-out instantly
        if (!couponRedisService.decrementCoupon(couponId)) {
            throw new IllegalStateException("Coupon is sold out (filtered by Redis).");
        }

        try {
            // Layer 2: Distributed Lock — one thread per couponId at a time
            return distributedLockExecutor.execute(
                    "lock:coupon:issue:" + couponId, 5, 3, TimeUnit.SECONDS,
                    () -> doIssueCoupon(couponId, memberId));
        } catch (Exception e) {
            // Redis counter was decremented but issuance failed — restore
            couponRedisService.restoreCoupon(couponId);
            throw e;
        }
    }

    private Long doIssueCoupon(Long couponId, Long memberId) {
        // Layer 3: DB Pessimistic Lock — final guarantee
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
