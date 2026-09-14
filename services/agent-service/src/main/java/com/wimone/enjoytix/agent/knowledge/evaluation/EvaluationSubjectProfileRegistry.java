package com.wimone.enjoytix.agent.knowledge.evaluation;

import java.util.Map;

public final class EvaluationSubjectProfileRegistry {
    private final Map<String, EvaluationSubjectProfile> profiles;

    public EvaluationSubjectProfileRegistry(Map<String, EvaluationSubjectProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) throw new IllegalArgumentException("evaluation profiles are required");
        this.profiles = Map.copyOf(profiles);
    }

    public EvaluationSubjectProfile resolve(String profileId) {
        EvaluationSubjectProfile profile = profiles.get(profileId);
        if (profile == null) throw new IllegalArgumentException("unknown evaluation subject profile: " + profileId);
        return profile;
    }
}
