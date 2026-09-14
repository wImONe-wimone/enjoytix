package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeCitation;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalQualityEvaluatorTest {
    @Test
    void reportsRecallRankingCitationCoverageAndNoHitPrecision() {
        RagProperties properties = new RagProperties();
        properties.setEvaluationRecallAtKThreshold(0.5);
        properties.setEvaluationCitationCoverageThreshold(1.0);
        KnowledgeRetrievalQualityEvaluator evaluator = new KnowledgeRetrievalQualityEvaluator(properties);
        KnowledgeCitation first = citation("cite-1", 0.95);
        KnowledgeCitation second = citation("cite-2", 0.8);

        KnowledgeRetrievalQualityReport report = evaluator.evaluate(List.of(
                new KnowledgeRetrievalQualityCase("q1", Set.of("cite-1", "cite-2"),
                        KnowledgeRetrievalResult.success(List.of(first, second), Map.of())),
                new KnowledgeRetrievalQualityCase("q2", Set.of(), KnowledgeRetrievalResult.noHit())));

        assertThat(report.recallAtK()).isEqualTo(1.0);
        assertThat(report.rankingAccuracy()).isEqualTo(1.0);
        assertThat(report.citationCoverage()).isEqualTo(1.0);
        assertThat(report.noHitPrecision()).isEqualTo(1.0);
        assertThat(report.thresholdsMet()).isTrue();
    }

    @Test
    void reportsFailedThresholdWhenRelevantCitationIsMissing() {
        RagProperties properties = new RagProperties();
        properties.setEvaluationRecallAtKThreshold(1.0);
        KnowledgeRetrievalQualityEvaluator evaluator = new KnowledgeRetrievalQualityEvaluator(properties);
        KnowledgeRetrievalQualityReport report = evaluator.evaluate(List.of(
                new KnowledgeRetrievalQualityCase("q1", Set.of("cite-1"),
                        KnowledgeRetrievalResult.success(List.of(citation("cite-2", 0.9)), Map.of()))));

        assertThat(report.recallAtK()).isZero();
        assertThat(report.thresholdsMet()).isFalse();
    }

    @Test
    void failsFirstRankGateWhenRelevantCitationIsNotFirst() {
        RagProperties properties = new RagProperties();
        properties.setEvaluationRankingThreshold(1.0);
        KnowledgeRetrievalQualityEvaluator evaluator = new KnowledgeRetrievalQualityEvaluator(properties);

        KnowledgeRetrievalQualityReport report = evaluator.evaluate(List.of(
                new KnowledgeRetrievalQualityCase("q1", Set.of("cite-1"),
                        KnowledgeRetrievalResult.success(List.of(citation("cite-2", 0.95), citation("cite-1", 0.8)), Map.of()))));

        assertThat(report.rankingAccuracy()).isZero();
        assertThat(report.thresholdsMet()).isFalse();
    }

    @Test
    void failsCitationCoverageGateWhenReturnedCitationsIncludeIrrelevantResults() {
        RagProperties properties = new RagProperties();
        properties.setEvaluationCitationCoverageThreshold(1.0);
        KnowledgeRetrievalQualityEvaluator evaluator = new KnowledgeRetrievalQualityEvaluator(properties);

        KnowledgeRetrievalQualityReport report = evaluator.evaluate(List.of(
                new KnowledgeRetrievalQualityCase("q1", Set.of("cite-1"),
                        KnowledgeRetrievalResult.success(List.of(citation("cite-1", 0.95), citation("cite-2", 0.8)), Map.of()))));

        assertThat(report.citationCoverage()).isEqualTo(0.5);
        assertThat(report.thresholdsMet()).isFalse();
    }

    @Test
    void failsNoHitPrecisionGateWhenUnexpectedResultsAreReturned() {
        RagProperties properties = new RagProperties();
        properties.setEvaluationNoHitPrecisionThreshold(1.0);
        KnowledgeRetrievalQualityEvaluator evaluator = new KnowledgeRetrievalQualityEvaluator(properties);

        KnowledgeRetrievalQualityReport report = evaluator.evaluate(List.of(
                new KnowledgeRetrievalQualityCase("q1", Set.of(),
                        KnowledgeRetrievalResult.success(List.of(citation("cite-1", 0.95)), Map.of()))));

        assertThat(report.noHitPrecision()).isZero();
        assertThat(report.thresholdsMet()).isFalse();
    }

    private KnowledgeCitation citation(String key, double score) {
        return new KnowledgeCitation(key, "kb-1", "doc-1", "v1", key, "Policy", "policy.md", score);
    }
}
