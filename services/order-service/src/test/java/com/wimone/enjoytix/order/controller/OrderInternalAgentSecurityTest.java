package com.wimone.enjoytix.order.controller;

import com.wimone.enjoytix.order.auth.AgentServiceAuthentication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderInternalAgentSecurityTest {
    @Test
    void rejectsMissingOrWrongServiceCredential() {
        AgentServiceAuthentication authentication = new AgentServiceAuthentication("expected-token");
        assertThatThrownBy(() -> authentication.requireValid("agent-service", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> authentication.requireValid("other-service", "expected-token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> authentication.requireValid("agent-service", "wrong-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsOnlyConfiguredAgentCredential() {
        new AgentServiceAuthentication("expected-token").requireValid("agent-service", "expected-token");
    }
}
