package com.wimone.enjoytix.agent.auth;

public record AgentUserContext(Long userId, String username) {
    public AgentUserContext {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
