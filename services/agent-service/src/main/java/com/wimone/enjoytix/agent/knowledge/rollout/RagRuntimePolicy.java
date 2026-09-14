package com.wimone.enjoytix.agent.knowledge.rollout;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import java.util.Objects;

public final class RagRuntimePolicy {
    public static final String DISABLED = "RAG_DISABLED";
    public static final String ROLLBACK = "RAG_ROLLBACK";
    public static final String OUTSIDE_COHORT = "OUTSIDE_RAG_COHORT";
    private final RagRolloutDecider decider;
    public RagRuntimePolicy(RagRolloutDecider decider) { this.decider = Objects.requireNonNull(decider, "decider is required"); }
    public RagRuntimeDecision decide(AgentUserContext user, String cohortKey) {
        Objects.requireNonNull(user, "user is required"); Objects.requireNonNull(cohortKey, "cohortKey is required");
        if (decider.isRollback()) return RagRuntimeDecision.fallback(cohortKey, ROLLBACK);
        if (!decider.isConfiguredEnabled()) return RagRuntimeDecision.fallback(cohortKey, DISABLED);
        return decider.isEnabled(user, cohortKey) ? RagRuntimeDecision.enabled(cohortKey) : RagRuntimeDecision.fallback(cohortKey, OUTSIDE_COHORT);
    }
}