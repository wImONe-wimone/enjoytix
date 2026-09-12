package com.wimone.enjoytix.order.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentServiceAuthentication {
    private final String expectedToken;

    public AgentServiceAuthentication(@Value("${order.agent.service-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void requireValid(String serviceName, String token) {
        if (!"agent-service".equals(serviceName) || expectedToken.isBlank() || token == null
                || !expectedToken.equals(token)) {
            throw new IllegalArgumentException("agent service authentication failed");
        }
    }
}
