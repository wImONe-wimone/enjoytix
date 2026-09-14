package com.wimone.enjoytix.agent.knowledge.rollout;

public record RagRuntimeDecision(boolean ragEnabled, String cohortKey, String fallbackReason) {
    public RagRuntimeDecision {
        if (cohortKey == null || cohortKey.isBlank()) throw new IllegalArgumentException("cohortKey is required");
        cohortKey = cohortKey.trim();
        fallbackReason = fallbackReason == null || fallbackReason.isBlank() ? null : fallbackReason.trim();
        if (ragEnabled && fallbackReason != null) throw new IllegalArgumentException("enabled decision cannot have fallback reason");
    }
    public static RagRuntimeDecision enabled(String cohortKey) { return new RagRuntimeDecision(true, cohortKey, null); }
    public static RagRuntimeDecision fallback(String cohortKey, String reason) { return new RagRuntimeDecision(false, cohortKey, reason); }
}