package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EvaluationEvidenceRecord(
        String contractVersion,
        String datasetId,
        String datasetVersion,
        String caseId,
        KnowledgeRetrievalOutcome retrievalOutcome,
        List<EvaluationEvidenceCitation> citations,
        List<EvaluationEvidenceContext> contexts,
        int candidateCount,
        int finalCount,
        long retrievalDurationMillis,
        EvaluationRetrievalConfiguration retrievalConfiguration,
        EvaluationDeterministicResult deterministicResult,
        EvaluationFailure failure) {
    public EvaluationEvidenceRecord {
        requireText(contractVersion, "evidence contract version");
        requireText(datasetId, "evidence dataset id");
        requireText(datasetVersion, "evidence dataset version");
        requireText(caseId, "evidence case id");
        retrievalOutcome = Objects.requireNonNull(retrievalOutcome, "retrieval outcome is required");
        citations = citations == null ? List.of() : List.copyOf(citations);
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
        if (citations.stream().anyMatch(Objects::isNull) || contexts.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("evidence entries are required");
        }
        if (candidateCount < 0 || finalCount < 0 || finalCount > candidateCount) {
            throw new IllegalArgumentException("evidence retrieval counts are invalid");
        }
        if (retrievalDurationMillis < 0) {
            throw new IllegalArgumentException("evidence retrieval duration must not be negative");
        }
        retrievalConfiguration = Objects.requireNonNull(retrievalConfiguration, "retrieval configuration is required");
        deterministicResult = Objects.requireNonNull(deterministicResult, "deterministic result is required");
        validateCitations(citations);
        validateContexts(citations, contexts);
        if (retrievalOutcome == KnowledgeRetrievalOutcome.NO_HIT && (!citations.isEmpty() || !contexts.isEmpty())) {
            throw new IllegalArgumentException("no-hit evidence cannot contain citations or contexts");
        }
        if (retrievalOutcome == KnowledgeRetrievalOutcome.FAILURE) {
            if (!citations.isEmpty() || !contexts.isEmpty() || failure == null) {
                throw new IllegalArgumentException("failed evidence must contain only a classified failure");
            }
        } else if (failure != null) {
            throw new IllegalArgumentException("non-failed evidence cannot contain a failure");
        }
    }

    private static void validateCitations(List<EvaluationEvidenceCitation> citations) {
        Set<String> citationKeys = new HashSet<>();
        for (int index = 0; index < citations.size(); index++) {
            EvaluationEvidenceCitation citation = citations.get(index);
            if (!citationKeys.add(citation.citationKey()) || citation.rank() != index + 1) {
                throw new IllegalArgumentException("evidence citations must have unique sequential ranks");
            }
        }
    }

    private static void validateContexts(List<EvaluationEvidenceCitation> citations,
                                         List<EvaluationEvidenceContext> contexts) {
        Set<String> citationKeys = citations.stream().map(EvaluationEvidenceCitation::citationKey).collect(java.util.stream.Collectors.toSet());
        if (contexts.stream().anyMatch(context -> !citationKeys.contains(context.citationKey()))) {
            throw new IllegalArgumentException("evidence context must reference an evidence citation");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
