package com.wimone.enjoytix.framework.cache.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RedissonDistributedLockTemplate implements DistributedLockTemplate {

    private final RedissonClient redissonClient;

    public RedissonDistributedLockTemplate(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new IllegalStateException("Failed to acquire distributed lock: " + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring distributed lock: " + lockKey, ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
