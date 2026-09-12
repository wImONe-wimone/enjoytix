package com.wimone.enjoytix.agent.auth;

public class AgentAuthenticationException extends RuntimeException {
    public AgentAuthenticationException(String message) {
        super(message);
    }

    public AgentAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
