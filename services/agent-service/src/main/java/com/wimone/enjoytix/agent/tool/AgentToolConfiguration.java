package com.wimone.enjoytix.agent.tool;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgentToolConfiguration {

    @Bean
    @ConditionalOnBean(ShowSessionQueryService.class)
    @ConditionalOnMissingBean(ShowSessionQueryTool.class)
    public ShowSessionQueryTool showSessionQueryTool(ShowSessionQueryService queryService) {
        return new ShowSessionQueryTool(queryService);
    }
}