package com.example.order.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility for executing business logic under a Redisson distributed lock.
 *
 * Usage:
 *   distributedLockExecutor.execute("lock:key", 5, 3, TimeUnit.SECONDS, () -> {
 *       // critical section
 *       return result;
 *   });
 *
 * - waitTime:  max time to wait for lock acquisition
 * - leaseTime: max time to hold the lock (auto-released after this)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

    private final RedissonClient redissonClient;

    public <T> T execute(String lockKey, long waitTime, long leaseTime,
                         TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new IllegalStateException(
                        "Failed to acquire lock: " + lockKey + " (waited " + waitTime + " " + timeUnit + ")");
            }
            log.debug("[DistributedLock] Acquired lock: {}", lockKey);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lock acquisition interrupted: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[DistributedLock] Released lock: {}", lockKey);
            }
        }
    }

    public void executeVoid(String lockKey, long waitTime, long leaseTime,
                            TimeUnit timeUnit, Runnable runnable) {
        execute(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }
}
