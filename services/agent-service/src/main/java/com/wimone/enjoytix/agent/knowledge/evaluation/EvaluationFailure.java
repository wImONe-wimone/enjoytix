package com.wimone.enjoytix.agent.knowledge.evaluation;

public record EvaluationFailure(String category, String safeMessage) {
    public EvaluationFailure {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("evaluation failure category is required");
        }
        if (safeMessage == null || safeMessage.isBlank()) {
            throw new IllegalArgumentException("evaluation failure message is required");
        }
    }
}
