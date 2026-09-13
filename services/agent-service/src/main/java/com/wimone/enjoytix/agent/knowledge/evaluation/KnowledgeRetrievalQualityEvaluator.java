package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class KnowledgeRetrievalQualityEvaluator {
    private final RagProperties properties;
    public KnowledgeRetrievalQualityEvaluator(RagProperties properties) { this.properties = Objects.requireNonNull(properties); }
    public KnowledgeRetrievalQualityReport evaluate(List<KnowledgeRetrievalQualityCase> cases) {
        if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("quality cases are required");
        double recall = 0, ranking = 0, coverage = 0, noHit = 0;
        for (KnowledgeRetrievalQualityCase qualityCase : cases) {
            Set<String> expected = qualityCase.expectedCitationKeys();
            List<String> actual = qualityCase.result().citations().stream().map(citation -> citation.citationKey()).toList();
            long hits = actual.stream().filter(expected::contains).distinct().count();
            recall += expected.isEmpty() ? (actual.isEmpty() ? 1 : 0) : (double) hits / expected.size();
            ranking += expected.isEmpty() ? (actual.isEmpty() ? 1 : 0) : (!actual.isEmpty() && expected.contains(actual.get(0)) ? 1 : 0);
            coverage += actual.isEmpty() ? (expected.isEmpty() ? 1 : 0) : actual.stream().filter(expected::contains).count() / (double) actual.size();
            noHit += expected.isEmpty() ? (qualityCase.result().outcome() == KnowledgeRetrievalOutcome.NO_HIT ? 1 : 0) : 1;
        }
        int size = cases.size();
        double recallScore = recall / size, rankingScore = ranking / size, coverageScore = coverage / size, noHitScore = noHit / size;
        boolean met = recallScore >= properties.getEvaluationRecallAtKThreshold()
                && rankingScore >= properties.getEvaluationRankingThreshold()
                && coverageScore >= properties.getEvaluationCitationCoverageThreshold()
                && noHitScore >= properties.getEvaluationNoHitPrecisionThreshold();
        return new KnowledgeRetrievalQualityReport(recallScore, rankingScore, coverageScore, noHitScore, met);
    }
}
