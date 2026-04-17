package com.example.order.service;

import com.example.order.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis layer for coupon remaining count — fast pre-filter before DB locks.
 *
 * Key format: "coupon:remain:{couponId}" → remaining issuable count
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String COUPON_KEY_PREFIX = "coupon:remain:";

    private final RedisTemplate<String, Long> redisTemplate;
    private final CouponRepository couponRepository;

    /**
     * Initialize coupon remaining count in Redis.
     * remaining = totalQuantity - issuedCount
     */
    public void syncCoupon(Long couponId, int totalQuantity, int issuedCount) {
        String key = COUPON_KEY_PREFIX + couponId;
        long remaining = totalQuantity - issuedCount;
        redisTemplate.opsForValue().set(key, remaining);
        log.info("[CouponRedis] Synced coupon:{} remain={}", couponId, remaining);
    }

    /**
     * Pre-decrement coupon count in Redis.
     * Returns true if coupon was available, false if sold out.
     */
    public boolean decrementCoupon(Long couponId) {
        String key = COUPON_KEY_PREFIX + couponId;

        Long remain = redisTemplate.opsForValue().decrement(key);

        if (remain == null) {
            return true; // cache miss — fall through to DB
        }

        if (remain < 0) {
            redisTemplate.opsForValue().increment(key);
            log.info("[CouponRedis] Rejected: coupon:{} sold out", couponId);
            return false;
        }

        log.debug("[CouponRedis] Decremented: coupon:{} remain={}", couponId, remain);
        return true;
    }

    /**
     * Restore coupon count in Redis (on issuance failure or order cancellation).
     */
    public void restoreCoupon(Long couponId) {
        String key = COUPON_KEY_PREFIX + couponId;
        redisTemplate.opsForValue().increment(key);
        log.debug("[CouponRedis] Restored: coupon:{} +1", couponId);
    }

    /**
     * Sync all coupons from DB to Redis on application startup.
     */
    public void syncAllFromDb() {
        couponRepository.findAll().forEach(coupon ->
                syncCoupon(coupon.getId(), coupon.getTotalQuantity(), coupon.getIssuedCount()));
        log.info("[CouponRedis] Synced all coupons from DB");
    }
}
