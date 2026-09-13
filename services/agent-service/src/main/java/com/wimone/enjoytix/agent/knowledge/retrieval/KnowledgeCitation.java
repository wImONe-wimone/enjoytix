package com.wimone.enjoytix.agent.knowledge.retrieval;

public record KnowledgeCitation(
        String citationKey,
        String knowledgeBaseId,
        String documentId,
        String versionId,
        String chunkId,
        String title,
        String sourceLocation,
        double score) {
    public KnowledgeCitation {
        requireText(citationKey, "citation key");
        requireText(knowledgeBaseId, "knowledge base id");
        requireText(documentId, "document id");
        requireText(versionId, "version id");
        requireText(chunkId, "chunk id");
        requireText(title, "citation title");
        requireText(sourceLocation, "source location");
        if (Double.isNaN(score) || score < 0 || score > 1) {
            throw new IllegalArgumentException("citation score must be between 0 and 1");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
