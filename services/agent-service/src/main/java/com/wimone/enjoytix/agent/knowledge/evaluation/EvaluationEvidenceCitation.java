package com.wimone.enjoytix.agent.knowledge.evaluation;

public record EvaluationEvidenceCitation(
        String citationKey,
        String knowledgeBaseId,
        String documentId,
        String versionId,
        String chunkId,
        int rank,
        double score) {
    public EvaluationEvidenceCitation {
        requireText(citationKey, "citation key");
        requireText(knowledgeBaseId, "knowledge base id");
        requireText(documentId, "document id");
        requireText(versionId, "version id");
        requireText(chunkId, "chunk id");
        if (rank <= 0) {
            throw new IllegalArgumentException("citation rank must be positive");
        }
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
