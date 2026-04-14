package com.example.order.repository;

import com.example.order.domain.CouponStatus;
import com.example.order.domain.MemberCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    /**
     * Pessimistic Lock for coupon usage.
     * Prevents concurrent use of the same coupon.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mc from MemberCoupon mc join fetch mc.coupon where mc.id = :id")
    Optional<MemberCoupon> findByIdWithLock(@Param("id") Long id);

    /**
     * Find member's coupons with coupon info (fetch join).
     */
    @Query("select mc from MemberCoupon mc join fetch mc.coupon where mc.member.id = :memberId")
    List<MemberCoupon> findAllByMemberIdWithCoupon(@Param("memberId") Long memberId);

    /**
     * Find member's coupons by status.
     */
    @Query("select mc from MemberCoupon mc join fetch mc.coupon " +
           "where mc.member.id = :memberId and mc.status = :status")
    List<MemberCoupon> findAllByMemberIdAndStatus(@Param("memberId") Long memberId,
                                                   @Param("status") CouponStatus status);

    /**
     * Find by used order ID (for coupon restoration on order cancel).
     */
    Optional<MemberCoupon> findByUsedOrderId(Long usedOrderId);

    /**
     * Check if member already has this coupon.
     */
    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);
}
