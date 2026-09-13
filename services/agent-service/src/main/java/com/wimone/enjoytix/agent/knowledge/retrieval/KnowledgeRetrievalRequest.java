package com.wimone.enjoytix.agent.knowledge.retrieval;

import com.wimone.enjoytix.agent.auth.AgentUserContext;

public record KnowledgeRetrievalRequest(
        AgentUserContext user,
        String query,
        int topK,
        double minimumScore) {
    public KnowledgeRetrievalRequest {
        if (user == null) {
            throw new IllegalArgumentException("authenticated user is required");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        query = query.trim();
        if (topK <= 0 || topK > 50) {
            throw new IllegalArgumentException("topK must be positive and at most 50");
        }
        if (Double.isNaN(minimumScore) || minimumScore < 0 || minimumScore > 1) {
            throw new IllegalArgumentException("minimumScore must be between 0 and 1");
        }
    }
}
