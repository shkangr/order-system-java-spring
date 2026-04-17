package com.example.order.service;

import com.example.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis layer for product stock — acts as a fast pre-filter before DB locks.
 *
 * Redis is NOT the source of truth. It holds approximate counts to reject
 * obviously-impossible requests early, saving DB connections and lock contention.
 * The DB Pessimistic Lock remains the final guarantee.
 *
 * Key format: "product:stock:{productId}" → remaining stock count
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockRedisService {

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    private final RedisTemplate<String, Long> redisTemplate;
    private final ProductRepository productRepository;

    /**
     * Load stock from DB into Redis (called on product creation or app startup).
     */
    public void syncStock(Long productId, int stockQuantity) {
        String key = STOCK_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(key, (long) stockQuantity);
        log.info("[StockRedis] Synced product:{} stock={}", productId, stockQuantity);
    }

    /**
     * Pre-decrement stock in Redis.
     * Returns true if stock was available (caller should proceed to DB lock).
     * Returns false if stock is exhausted (caller should reject immediately).
     *
     * If Redis has no key (cache miss), returns true to fall through to DB.
     */
    public boolean decrementStock(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;

        Long remain = redisTemplate.opsForValue().decrement(key, quantity);

        // Cache miss — key doesn't exist, Redis returns negative from 0
        if (remain == null) {
            return true; // fall through to DB
        }

        if (remain < 0) {
            // Restore: we over-decremented
            redisTemplate.opsForValue().increment(key, quantity);
            log.info("[StockRedis] Rejected: product:{} insufficient stock (remain after restore: ~{})",
                    productId, remain + quantity);
            return false;
        }

        log.debug("[StockRedis] Decremented: product:{} remain={}", productId, remain);
        return true;
    }

    /**
     * Restore stock in Redis (called on order cancel or DB transaction rollback).
     */
    public void restoreStock(Long productId, int quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        redisTemplate.opsForValue().increment(key, quantity);
        log.debug("[StockRedis] Restored: product:{} +{}", productId, quantity);
    }

    /**
     * Sync all products from DB to Redis.
     * Called on application startup to ensure Redis matches DB state.
     */
    public void syncAllFromDb() {
        productRepository.findAll().forEach(product ->
                syncStock(product.getId(), product.getStockQuantity()));
        log.info("[StockRedis] Synced all products from DB");
    }
}
