package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record EvaluationRetrievalRequest(String datasetId, List<String> caseIds) {
    public EvaluationRetrievalRequest {
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("evaluation dataset id is required");
        }
        datasetId = datasetId.trim();
        caseIds = caseIds == null ? List.of() : List.copyOf(caseIds);
        if (caseIds.stream().anyMatch(caseId -> caseId == null || caseId.isBlank())) {
            throw new IllegalArgumentException("evaluation case id is required");
        }
        if (caseIds.stream().distinct().count() != caseIds.size()) {
            throw new IllegalArgumentException("evaluation case ids must be unique");
        }
    }
}
