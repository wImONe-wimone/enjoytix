package com.wimone.enjoytix.agent.auth;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "agent.auth.sa-token.enabled", havingValue = "true")
public class SaTokenAgentAuthenticationAdapter implements AgentAuthenticationPort {
    @Override
    public AgentUserContext resolve(HttpServletRequest request) {
        if (!StpUtil.isLogin()) {
            throw new AgentAuthenticationException("Authenticated Sa-Token login is required");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        Object username = StpUtil.getExtra("username");
        return new AgentUserContext(userId, username == null ? null : username.toString());
    }
}
