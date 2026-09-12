package com.wimone.enjoytix.agent.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class AgentFeignUserContextInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        AgentUserContext context = AgentUserContextHolder.current();
        if (context == null) {
            return;
        }
        template.header(HeaderAgentAuthenticationAdapter.USER_ID_HEADER, context.userId().toString());
        if (context.username() != null && !context.username().isBlank()) {
            template.header(HeaderAgentAuthenticationAdapter.USERNAME_HEADER, context.username());
        }
    }
}
