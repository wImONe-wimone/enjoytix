package com.wimone.enjoytix.agent.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.knowledge.rag")
public class RagProperties {
    private boolean indexingEnabled;
    private boolean retrievalEnabled;
    private boolean rerankingEnabled;
    private boolean citationsEnabled;
    private int topK = 5;
    private int candidateLimit = 20;
    private double minimumScore = 0.65;
    private String embeddingModel = "text-embedding-v3";
    private int embeddingDimension = 1024;
    private double evaluationRecallAtKThreshold = 0.8;
    private double evaluationRankingThreshold = 0.8;
    private double evaluationCitationCoverageThreshold = 0.8;
    private double evaluationNoHitPrecisionThreshold = 0.9;

    public boolean isIndexingEnabled() { return indexingEnabled; }
    public void setIndexingEnabled(boolean indexingEnabled) { this.indexingEnabled = indexingEnabled; }
    public boolean isRetrievalEnabled() { return retrievalEnabled; }
    public void setRetrievalEnabled(boolean retrievalEnabled) { this.retrievalEnabled = retrievalEnabled; }
    public boolean isRerankingEnabled() { return rerankingEnabled; }
    public void setRerankingEnabled(boolean rerankingEnabled) { this.rerankingEnabled = rerankingEnabled; }
    public boolean isCitationsEnabled() { return citationsEnabled; }
    public void setCitationsEnabled(boolean citationsEnabled) { this.citationsEnabled = citationsEnabled; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public int getCandidateLimit() { return candidateLimit; }
    public void setCandidateLimit(int candidateLimit) { this.candidateLimit = candidateLimit; }
    public double getMinimumScore() { return minimumScore; }
    public void setMinimumScore(double minimumScore) { this.minimumScore = minimumScore; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public double getEvaluationRecallAtKThreshold() { return evaluationRecallAtKThreshold; }
    public void setEvaluationRecallAtKThreshold(double value) { evaluationRecallAtKThreshold = bounded(value); }
    public double getEvaluationRankingThreshold() { return evaluationRankingThreshold; }
    public void setEvaluationRankingThreshold(double value) { evaluationRankingThreshold = bounded(value); }
    public double getEvaluationCitationCoverageThreshold() { return evaluationCitationCoverageThreshold; }
    public void setEvaluationCitationCoverageThreshold(double value) { evaluationCitationCoverageThreshold = bounded(value); }
    public double getEvaluationNoHitPrecisionThreshold() { return evaluationNoHitPrecisionThreshold; }
    public void setEvaluationNoHitPrecisionThreshold(double value) { evaluationNoHitPrecisionThreshold = bounded(value); }
    private double bounded(double value) { if (Double.isNaN(value) || value < 0 || value > 1) throw new IllegalArgumentException("evaluation threshold must be between 0 and 1"); return value; }

    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
}
