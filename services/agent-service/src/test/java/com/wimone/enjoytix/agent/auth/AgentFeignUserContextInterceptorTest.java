package com.wimone.enjoytix.agent.auth;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentFeignUserContextInterceptorTest {
    @AfterEach
    void clearContext() { AgentUserContextHolder.clear(); }

    @Test
    void injectsOnlyTrustedContext() {
        AgentUserContextHolder.set(new AgentUserContext(1001L, "alice"));
        RequestTemplate template = new RequestTemplate();
        new AgentFeignUserContextInterceptor().apply(template);
        assertThat(template.headers().get("X-User-Id")).containsExactly("1001");
        assertThat(template.headers().get("X-Username")).containsExactly("alice");
    }

    @Test
    void doesNotInjectHeadersWithoutContext() {
        RequestTemplate template = new RequestTemplate();
        new AgentFeignUserContextInterceptor().apply(template);
        assertThat(template.headers()).doesNotContainKeys("X-User-Id", "X-Username");
    }
}
