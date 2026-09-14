package com.wimone.enjoytix.agent.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public record EvaluationDataset(
        String datasetId,
        String schemaVersion,
        String datasetVersion,
        List<RetrievalEvaluationCase> cases) {
    public EvaluationDataset {
        if (datasetId == null || datasetId.isBlank()) throw new IllegalArgumentException("dataset id is required");
        if (schemaVersion == null || schemaVersion.isBlank()) throw new IllegalArgumentException("schema version is required");
        if (datasetVersion == null || datasetVersion.isBlank()) throw new IllegalArgumentException("dataset version is required");
        cases = cases == null ? List.of() : List.copyOf(cases);
        if (cases.isEmpty()) throw new IllegalArgumentException("evaluation cases are required");
        if (cases.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("evaluation case is required");
        if (cases.stream().anyMatch(item -> !schemaVersion.equals(item.schemaVersion()))) {
            throw new IllegalArgumentException("inconsistent schema version");
        }
        if (cases.stream().anyMatch(item -> !datasetVersion.equals(item.datasetVersion()))) {
            throw new IllegalArgumentException("inconsistent dataset version");
        }
    }

    public RetrievalEvaluationCase caseById(String caseId) {
        return cases.stream().filter(item -> item.caseId().equals(caseId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown caseId: " + caseId));
    }
}
