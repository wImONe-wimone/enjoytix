package com.wimone.enjoytix.agent.knowledge.domain;

public record KnowledgeBase(
        String id,
        String name,
        String embeddingModel,
        int embeddingDimension,
        boolean enabled) {
    public KnowledgeBase {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("knowledge base id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("knowledge base name is required");
        if (embeddingModel == null || embeddingModel.isBlank()) throw new IllegalArgumentException("embedding model is required");
        if (embeddingDimension <= 0) throw new IllegalArgumentException("embedding dimension must be positive");
    }
}
