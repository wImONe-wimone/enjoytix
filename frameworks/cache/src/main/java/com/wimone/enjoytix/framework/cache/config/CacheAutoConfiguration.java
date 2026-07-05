package com.wimone.enjoytix.framework.cache.config;

import com.wimone.enjoytix.framework.cache.DistributedCache;
import com.wimone.enjoytix.framework.cache.lock.DistributedLockTemplate;
import com.wimone.enjoytix.framework.cache.lock.LocalDistributedLockTemplate;
import com.wimone.enjoytix.framework.cache.lock.RedissonDistributedLockTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public DistributedCache distributedCache(StringRedisTemplate stringRedisTemplate) {
        return new DistributedCache(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "enjoytix.lock", name = "type", havingValue = "redisson")
    @ConditionalOnMissingBean
    public DistributedLockTemplate redissonDistributedLockTemplate(RedissonClient redissonClient) {
        return new RedissonDistributedLockTemplate(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockTemplate localDistributedLockTemplate() {
        return new LocalDistributedLockTemplate();
    }
}
