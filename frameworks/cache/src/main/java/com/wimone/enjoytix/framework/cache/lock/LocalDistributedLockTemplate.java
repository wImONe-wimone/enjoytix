package com.wimone.enjoytix.framework.cache.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class LocalDistributedLockTemplate implements DistributedLockTemplate {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, ignored -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, timeUnit);
            if (!acquired) {
                throw new IllegalStateException("Failed to acquire distributed lock: " + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring distributed lock: " + lockKey, ex);
        } finally {
            if (acquired) {
                lock.unlock();
                if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                    locks.remove(lockKey, lock);
                }
            }
        }
    }
}
