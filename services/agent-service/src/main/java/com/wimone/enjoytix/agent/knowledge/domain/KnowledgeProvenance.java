package com.wimone.enjoytix.agent.knowledge.domain;

public record KnowledgeProvenance(String sourceName, Integer page, String section) {
    public KnowledgeProvenance {
        if (sourceName == null || sourceName.isBlank()) throw new IllegalArgumentException("source name is required");
        if (page != null && page < 1) throw new IllegalArgumentException("page must be positive");
    }
}
