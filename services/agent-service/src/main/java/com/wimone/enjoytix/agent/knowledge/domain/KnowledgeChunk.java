package com.wimone.enjoytix.agent.knowledge.domain;

import java.util.Map;

public record KnowledgeChunk(
        String id,
        String versionId,
        int ordinal,
        String content,
        String contentHash,
        int characterCount,
        int tokenCount,
        Map<String, String> metadata,
        KnowledgeProvenance provenance) {
    public KnowledgeChunk {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("chunk id is required");
        if (versionId == null || versionId.isBlank()) throw new IllegalArgumentException("version id is required");
        if (ordinal < 0) throw new IllegalArgumentException("chunk ordinal cannot be negative");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("chunk content is required");
        if (contentHash == null || contentHash.isBlank()) throw new IllegalArgumentException("chunk content hash is required");
        if (characterCount < 0 || tokenCount < 0) throw new IllegalArgumentException("chunk counts cannot be negative");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
