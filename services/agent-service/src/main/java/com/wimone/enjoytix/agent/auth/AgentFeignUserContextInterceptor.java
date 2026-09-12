package com.wimone.enjoytix.agent.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class AgentFeignUserContextInterceptor implements RequestInterceptor {
    @Value("${agent.service-token:}")
    private String serviceToken;

    @Override
    public void apply(RequestTemplate template) {
        AgentUserContext context = AgentUserContextHolder.current();
        if (context == null) {
            return;
        }
        template.header(HeaderAgentAuthenticationAdapter.USER_ID_HEADER, context.userId().toString());
        template.header("X-Agent-Service", "agent-service");
        if (serviceToken != null && !serviceToken.isBlank()) {
            template.header("X-Agent-Token", serviceToken);
        }
        if (context.username() != null && !context.username().isBlank()) {
            template.header(HeaderAgentAuthenticationAdapter.USERNAME_HEADER, context.username());
        }
    }
}
