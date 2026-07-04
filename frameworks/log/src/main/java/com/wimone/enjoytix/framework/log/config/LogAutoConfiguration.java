package com.wimone.enjoytix.framework.log.config;

import com.wimone.enjoytix.framework.log.core.OperationLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OperationLogAspect operationLogAspect() {
        return new OperationLogAspect();
    }
}
