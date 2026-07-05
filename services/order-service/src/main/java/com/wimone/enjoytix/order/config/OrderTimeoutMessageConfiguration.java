package com.wimone.enjoytix.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class OrderTimeoutMessageConfiguration {

    @Bean
    public ThreadPoolTaskScheduler orderTimeoutTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("order-timeout-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
