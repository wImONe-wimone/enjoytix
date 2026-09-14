package com.wimone.enjoytix.agent.knowledge.evaluation;

import java.util.Set;

public record RetrievalEvaluationCase(
        String schemaVersion,
        String datasetVersion,
        String caseId,
        String query,
        String subjectProfileId,
        EvaluationExpectedOutcome expectedOutcome,
        Set<String> expectedCitationKeys,
        String referenceAnswer,
        Set<String> tags) {
    public RetrievalEvaluationCase {
        requireText(schemaVersion, "schema version");
        requireText(datasetVersion, "dataset version");
        requireText(caseId, "case id");
        requireText(query, "query");
        requireText(subjectProfileId, "subject profile id");
        if (expectedOutcome == null) {
            throw new IllegalArgumentException("expected outcome is required");
        }
        if (expectedCitationKeys == null) {
            throw new IllegalArgumentException("expected citation keys are required");
        }
        expectedCitationKeys = Set.copyOf(expectedCitationKeys);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        if (expectedOutcome == EvaluationExpectedOutcome.SUCCESS && (referenceAnswer == null || referenceAnswer.isBlank())) {
            throw new IllegalArgumentException("successful case reference answer is required");
        }
        if (expectedOutcome == EvaluationExpectedOutcome.NO_HIT && !expectedCitationKeys.isEmpty()) {
            throw new IllegalArgumentException("no-hit case must not declare expected citations");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
