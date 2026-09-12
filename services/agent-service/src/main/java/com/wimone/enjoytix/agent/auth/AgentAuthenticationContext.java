package com.wimone.enjoytix.agent.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AgentAuthenticationContext {
    private final AgentAuthenticationPort authenticationPort;

    public AgentAuthenticationContext(AgentAuthenticationPort authenticationPort) {
        this.authenticationPort = authenticationPort;
    }

    public AgentUserContext resolve(HttpServletRequest request) {
        return authenticationPort.resolve(request);
    }
}
