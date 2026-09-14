package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.auth.AgentUserContext;

public record EvaluationSubjectProfile(String profileId, AgentUserContext user) {
    public EvaluationSubjectProfile {
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profile id is required");
        if (user == null) throw new IllegalArgumentException("evaluation user is required");
    }
}
