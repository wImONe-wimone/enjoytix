package com.wimone.enjoytix.framework.idempotent.config;

import com.wimone.enjoytix.framework.idempotent.core.IdempotentAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(StringRedisTemplate stringRedisTemplate) {
        return new IdempotentAspect(stringRedisTemplate);
    }
}
