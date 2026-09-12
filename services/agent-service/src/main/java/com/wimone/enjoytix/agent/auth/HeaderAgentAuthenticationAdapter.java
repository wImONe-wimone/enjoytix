package com.wimone.enjoytix.agent.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(SaTokenAgentAuthenticationAdapter.class)
public class HeaderAgentAuthenticationAdapter implements AgentAuthenticationPort {
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USERNAME_HEADER = "X-Username";

    @Override
    public AgentUserContext resolve(HttpServletRequest request) {
        String rawUserId = request.getHeader(USER_ID_HEADER);
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new AgentAuthenticationException("Authenticated user context is required");
        }
        try {
            return new AgentUserContext(Long.valueOf(rawUserId), request.getHeader(USERNAME_HEADER));
        } catch (NumberFormatException ex) {
            throw new AgentAuthenticationException("Authenticated user context is invalid", ex);
        }
    }
}
