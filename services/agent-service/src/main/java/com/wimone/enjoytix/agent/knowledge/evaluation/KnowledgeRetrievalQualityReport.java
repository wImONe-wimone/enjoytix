package com.wimone.enjoytix.agent.knowledge.evaluation;

public record KnowledgeRetrievalQualityReport(double recallAtK, double rankingAccuracy, double citationCoverage, double noHitPrecision, boolean thresholdsMet) {}
