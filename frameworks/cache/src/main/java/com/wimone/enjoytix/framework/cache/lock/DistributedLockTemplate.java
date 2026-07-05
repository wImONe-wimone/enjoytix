package com.wimone.enjoytix.framework.cache.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface DistributedLockTemplate {

    <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier);
}
