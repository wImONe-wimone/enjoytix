package com.wimone.enjoytix.agent.auth;

import jakarta.servlet.http.HttpServletRequest;

public interface AgentAuthenticationPort {
    AgentUserContext resolve(HttpServletRequest request);
}
