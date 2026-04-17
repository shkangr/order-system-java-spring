package com.example.order.event;

import com.example.order.domain.MemberCoupon;
import com.example.order.repository.MemberCouponRepository;
import com.example.order.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for order events and handles coupon-related side effects.
 *
 * Uses @TransactionalEventListener(BEFORE_COMMIT) so that coupon restoration
 * happens within the same transaction as the order cancellation.
 * If restoration fails, the entire cancel transaction rolls back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventListener {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRedisService couponRedisService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (event.getMemberCouponId() == null) {
            return;
        }

        log.info("[CouponEvent] Restoring coupon for cancelled order. orderId={}, memberCouponId={}",
                event.getOrderId(), event.getMemberCouponId());

        MemberCoupon memberCoupon = memberCouponRepository.findById(event.getMemberCouponId())
                .orElse(null);

        if (memberCoupon == null) {
            log.warn("[CouponEvent] MemberCoupon not found. id={}", event.getMemberCouponId());
            return;
        }

        memberCoupon.restore();
        memberCoupon.getCoupon().restoreQuantity();

        // Restore coupon count in Redis to keep cache in sync
        couponRedisService.restoreCoupon(memberCoupon.getCoupon().getId());

        log.info("[CouponEvent] Coupon restored (DB + Redis). memberCouponId={}, coupon={}",
                memberCoupon.getId(), memberCoupon.getCoupon().getName());
    }
}
