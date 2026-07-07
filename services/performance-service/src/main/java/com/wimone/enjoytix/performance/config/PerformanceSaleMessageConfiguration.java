package com.wimone.enjoytix.performance.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableConfigurationProperties(PerformanceSaleMessageProperties.class)
public class PerformanceSaleMessageConfiguration {

    @Bean
    @Profile("test")
    public ThreadPoolTaskScheduler performanceSaleTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("performance-sale-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
