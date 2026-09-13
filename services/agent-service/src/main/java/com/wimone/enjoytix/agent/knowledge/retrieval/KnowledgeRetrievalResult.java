package com.wimone.enjoytix.agent.knowledge.retrieval;

import java.util.List;
import java.util.Map;

public record KnowledgeRetrievalResult(
        KnowledgeRetrievalOutcome outcome,
        List<KnowledgeCitation> citations,
        Map<String, String> metadata,
        String safeMessage) {
    public KnowledgeRetrievalResult {
        if (outcome == null) {
            throw new IllegalArgumentException("retrieval outcome is required");
        }
        citations = citations == null ? List.of() : List.copyOf(citations);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (safeMessage != null && safeMessage.contains("api-key")) {
            throw new IllegalArgumentException("safe message must not contain credentials");
        }
    }

    public static KnowledgeRetrievalResult success(List<KnowledgeCitation> citations,
                                                   Map<String, String> metadata) {
        return new KnowledgeRetrievalResult(KnowledgeRetrievalOutcome.SUCCESS, citations, metadata, null);
    }

    public static KnowledgeRetrievalResult noHit() {
        return new KnowledgeRetrievalResult(KnowledgeRetrievalOutcome.NO_HIT, List.of(), Map.of(), null);
    }

    public static KnowledgeRetrievalResult failure(Throwable ignored) {
        return failure(ignored, "UNKNOWN");
    }

    public static KnowledgeRetrievalResult failure(Throwable ignored, String category) {
        return new KnowledgeRetrievalResult(KnowledgeRetrievalOutcome.FAILURE, List.of(),
                Map.of("failureCategory", category == null || category.isBlank() ? "UNKNOWN" : category),
                "Knowledge retrieval is temporarily unavailable.");
    }}
