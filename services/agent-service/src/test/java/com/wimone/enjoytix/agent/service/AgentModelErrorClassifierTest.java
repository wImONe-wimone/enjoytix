package com.wimone.enjoytix.agent.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AgentModelErrorClassifierTest {

    private final AgentModelErrorClassifier classifier = new AgentModelErrorClassifier();

    @Test
    void classifiesTimeoutWithoutLeakingProviderDetails() {
        AgentModelFailure failure = classifier.classify(
                new IllegalStateException("api-key=secret-value", new TimeoutException("provider stack detail")));

        assertThat(failure.category()).isEqualTo(AgentModelFailureCategory.TIMEOUT);
        assertThat(failure.safeMessage()).doesNotContain("secret-value", "provider stack detail");
    }

    @Test
    void classifiesProviderErrorsWithoutLeakingRawMessage() {
        AgentModelFailure failure = classifier.classify(new IllegalStateException("Bearer private-token"));

        assertThat(failure.category()).isEqualTo(AgentModelFailureCategory.PROVIDER_ERROR);
        assertThat(failure.safeMessage()).doesNotContain("private-token");
    }
}
