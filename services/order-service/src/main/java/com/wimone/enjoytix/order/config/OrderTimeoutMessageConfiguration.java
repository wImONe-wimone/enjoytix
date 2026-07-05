package com.wimone.enjoytix.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(OrderTimeoutMessageProperties.class)
public class OrderTimeoutMessageConfiguration {

    @Bean
    @Profile("test")
    public ThreadPoolTaskScheduler orderTimeoutTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("order-timeout-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
