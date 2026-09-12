package com.wimone.enjoytix.agent.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderAgentAuthenticationAdapterTest {
    @Test
    void resolvesGatewayUserContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1001");
        request.addHeader("X-Username", "alice");
        assertThat(new HeaderAgentAuthenticationAdapter().resolve(request))
                .isEqualTo(new AgentUserContext(1001L, "alice"));
    }
}
