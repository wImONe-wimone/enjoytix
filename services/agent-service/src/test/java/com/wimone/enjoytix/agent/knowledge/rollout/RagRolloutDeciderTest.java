package com.wimone.enjoytix.agent.knowledge.rollout;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagRolloutDeciderTest {
    @Test
    void keepsCohortDecisionStableAndSupportsInternalUsers() {
        RagRolloutProperties properties = new RagRolloutProperties();
        properties.setEnabled(true);
        properties.setCanaryPercentage(50);
        properties.setInternalUserIds("7,8");
        RagRolloutDecider decider = new RagRolloutDecider(properties);

        assertThat(decider.isEnabled(new AgentUserContext(7L, "alice"), "conversation-1")).isTrue();
        assertThat(decider.isEnabled(new AgentUserContext(123L, "bob"), "conversation-2"))
                .isEqualTo(decider.isEnabled(new AgentUserContext(123L, "bob"), "conversation-2"));
    }

    @Test
    void disablesRagForRollbackAndFallsBackToPreRagPath() {
        RagRolloutProperties properties = new RagRolloutProperties();
        properties.setEnabled(true);
        properties.setCanaryPercentage(100);
        properties.setRollback(true);

        assertThat(new RagRolloutDecider(properties).isEnabled(new AgentUserContext(7L, "alice"), "run-1"))
                .isFalse();
    }

    @Test
    void restartUsesPersistedConfigurationWithoutInMemoryState() {
        RagRolloutProperties first = new RagRolloutProperties();
        first.setEnabled(false);
        RagRolloutDecider beforeRestart = new RagRolloutDecider(first);
        RagRolloutProperties afterRestart = new RagRolloutProperties();
        afterRestart.setEnabled(true);
        afterRestart.setCanaryPercentage(100);

        assertThat(beforeRestart.isEnabled(new AgentUserContext(7L, "alice"), "run-1")).isFalse();
        assertThat(new RagRolloutDecider(afterRestart).isEnabled(new AgentUserContext(7L, "alice"), "run-1"))
                .isTrue();
    }
}