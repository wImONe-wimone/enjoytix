package com.wimone.enjoytix.agent.knowledge.evaluation;

public record EvaluationRetrievalConfiguration(
        boolean retrievalEnabled,
        boolean rerankingEnabled,
        int topK,
        int candidateLimit,
        double minimumScore,
        String embeddingModel,
        int embeddingDimension) {
    public EvaluationRetrievalConfiguration {
        if (topK <= 0 || topK > 50) {
            throw new IllegalArgumentException("evaluation topK must be positive and at most 50");
        }
        if (candidateLimit < topK || candidateLimit > 50) {
            throw new IllegalArgumentException("evaluation candidate limit must be between topK and 50");
        }
        if (Double.isNaN(minimumScore) || minimumScore < 0 || minimumScore > 1) {
            throw new IllegalArgumentException("evaluation minimum score must be between 0 and 1");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("evaluation embedding model is required");
        }
        if (embeddingDimension <= 0) {
            throw new IllegalArgumentException("evaluation embedding dimension must be positive");
        }
    }
}
