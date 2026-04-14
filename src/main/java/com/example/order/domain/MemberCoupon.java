package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private Long usedOrderId;

    private LocalDateTime usedAt;

    // === Factory Method === //

    public static MemberCoupon issue(Member member, Coupon coupon) {
        MemberCoupon memberCoupon = new MemberCoupon();
        memberCoupon.member = member;
        memberCoupon.coupon = coupon;
        memberCoupon.status = CouponStatus.ISSUED;
        return memberCoupon;
    }

    // === Business Logic === //

    /**
     * Use coupon for an order.
     */
    public void use(Long orderId) {
        if (this.status != CouponStatus.ISSUED) {
            throw new IllegalStateException("Coupon is not in ISSUED status. current=" + this.status);
        }
        if (!this.coupon.isValid()) {
            throw new IllegalStateException("Coupon is no longer valid.");
        }
        this.status = CouponStatus.USED;
        this.usedOrderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * Restore coupon (when order is cancelled).
     */
    public void restore() {
        if (this.status != CouponStatus.USED) {
            throw new IllegalStateException("Only USED coupons can be restored. current=" + this.status);
        }
        this.status = CouponStatus.ISSUED;
        this.usedOrderId = null;
        this.usedAt = null;
    }

    /**
     * Expire coupon.
     */
    public void expire() {
        if (this.status != CouponStatus.ISSUED) {
            throw new IllegalStateException("Only ISSUED coupons can be expired. current=" + this.status);
        }
        this.status = CouponStatus.EXPIRED;
    }
}
