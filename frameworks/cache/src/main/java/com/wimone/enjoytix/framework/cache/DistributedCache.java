package com.wimone.enjoytix.framework.cache;

import com.alibaba.fastjson2.JSON;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DistributedCache {

    private final StringRedisTemplate stringRedisTemplate;

    public DistributedCache(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public StringRedisTemplate getInstance() {
        return stringRedisTemplate;
    }

    public boolean hasKey(String key) {
        Boolean result = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(value), timeout, timeUnit);
    }

    public <T> T get(String key, Class<T> targetClass) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return JSON.parseObject(value, targetClass);
    }

    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    public <T> T safeGet(String key, Class<T> targetClass, Supplier<T> loader, long timeout, TimeUnit timeUnit) {
        T cacheValue = get(key, targetClass);
        if (cacheValue != null) {
            return cacheValue;
        }
        T loadedValue = loader.get();
        if (loadedValue != null) {
            set(key, loadedValue, timeout, timeUnit);
        }
        return loadedValue;
    }
}
