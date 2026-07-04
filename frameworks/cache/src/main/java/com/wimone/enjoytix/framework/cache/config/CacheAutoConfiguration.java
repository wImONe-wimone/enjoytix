package com.wimone.enjoytix.framework.cache.config;

import com.wimone.enjoytix.framework.cache.DistributedCache;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
}
