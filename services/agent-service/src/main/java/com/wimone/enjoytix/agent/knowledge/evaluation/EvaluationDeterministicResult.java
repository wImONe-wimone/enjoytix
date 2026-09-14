package com.wimone.enjoytix.agent.knowledge.evaluation;

public record EvaluationDeterministicResult(
        String metricVersion,
        double recallAtK,
        double rankingAccuracy,
        double citationCoverage,
        double noHitPrecision,
        boolean thresholdsMet) {
    public EvaluationDeterministicResult {
        if (metricVersion == null || metricVersion.isBlank()) {
            throw new IllegalArgumentException("deterministic metric version is required");
        }
        requireScore(recallAtK, "recall at K");
        requireScore(rankingAccuracy, "ranking accuracy");
        requireScore(citationCoverage, "citation coverage");
        requireScore(noHitPrecision, "no-hit precision");
    }

    private static void requireScore(double value, String field) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }
}
