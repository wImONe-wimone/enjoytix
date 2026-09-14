package com.wimone.enjoytix.agent.knowledge.rollout;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RagRuntimePolicyTest {
    @Test void explainsDisabledAndRollbackDecisions() {
        RagRolloutProperties properties = new RagRolloutProperties();
        RagRuntimePolicy policy = new RagRuntimePolicy(new RagRolloutDecider(properties));
        assertThat(policy.decide(new AgentUserContext(7L, "alice"), "conversation-1")).isEqualTo(RagRuntimeDecision.fallback("conversation-1", RagRuntimePolicy.DISABLED));
        properties.setEnabled(true); properties.setRollback(true);
        assertThat(new RagRuntimePolicy(new RagRolloutDecider(properties)).decide(new AgentUserContext(7L, "alice"), "conversation-1").fallbackReason()).isEqualTo(RagRuntimePolicy.ROLLBACK);
    }
    @Test void distinguishesEnabledAndOutsideCohort() {
        RagRolloutProperties properties = new RagRolloutProperties(); properties.setEnabled(true); properties.setCanaryPercentage(100);
        RagRuntimePolicy policy = new RagRuntimePolicy(new RagRolloutDecider(properties));
        assertThat(policy.decide(new AgentUserContext(7L, "alice"), "conversation-1").ragEnabled()).isTrue();
        properties.setCanaryPercentage(0);
        assertThat(policy.decide(new AgentUserContext(7L, "alice"), "conversation-1").fallbackReason()).isEqualTo(RagRuntimePolicy.OUTSIDE_COHORT);
    }
}