package com.wimone.enjoytix.agent.knowledge.evaluation;

public record EvaluationEvidenceContext(String citationKey, String content) {
    public EvaluationEvidenceContext {
        if (citationKey == null || citationKey.isBlank()) {
            throw new IllegalArgumentException("context citation key is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("retrieved context is required");
        }
    }
}
