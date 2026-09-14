package com.wimone.enjoytix.agent.knowledge.evaluation;

import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.retrieval.AuthorizedKnowledgeRetrievalService;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public final class EvaluationRetrievalAdapter {
    private static final String EVIDENCE_CONTRACT_VERSION = "1";
    private static final String DETERMINISTIC_METRIC_VERSION = "1";

    private final EvaluationDatasetRegistry datasetRegistry;
    private final EvaluationSubjectProfileRegistry subjectProfileRegistry;
    private final AuthorizedKnowledgeRetrievalService retrievalService;
    private final RagProperties ragProperties;
    private final KnowledgeRetrievalQualityEvaluator qualityEvaluator;

    public EvaluationRetrievalAdapter(EvaluationDatasetRegistry datasetRegistry,
                                      EvaluationSubjectProfileRegistry subjectProfileRegistry,
                                      AuthorizedKnowledgeRetrievalService retrievalService,
                                      RagProperties ragProperties) {
        this.datasetRegistry = Objects.requireNonNull(datasetRegistry, "evaluation dataset registry is required");
        this.subjectProfileRegistry = Objects.requireNonNull(subjectProfileRegistry, "evaluation subject profile registry is required");
        this.retrievalService = Objects.requireNonNull(retrievalService, "authorized retrieval service is required");
        this.ragProperties = Objects.requireNonNull(ragProperties, "RAG properties are required");
        this.qualityEvaluator = new KnowledgeRetrievalQualityEvaluator(ragProperties);
    }

    public EvaluationEvidenceRecord evaluate(String datasetId, String caseId) {
        EvaluationDataset dataset = datasetRegistry.resolve(datasetId);
        RetrievalEvaluationCase evaluationCase = dataset.caseById(caseId);
        EvaluationSubjectProfile subjectProfile = subjectProfileRegistry.resolve(evaluationCase.subjectProfileId());
        EvaluationRetrievalConfiguration retrievalConfiguration = retrievalConfiguration();
        KnowledgeRetrievalResult retrievalResult;
        long started = System.nanoTime();
        try {
            retrievalResult = retrievalService.retrieve(new KnowledgeRetrievalRequest(
                    subjectProfile.user(), evaluationCase.query(), ragProperties.getTopK(), ragProperties.getMinimumScore()));
        } catch (RuntimeException failure) {
            retrievalResult = KnowledgeRetrievalResult.failure(failure, "ADAPTER");
        }
        long durationMillis = Math.max(0, (System.nanoTime() - started) / 1_000_000);
        return toEvidence(dataset, evaluationCase, retrievalResult, durationMillis, retrievalConfiguration);
    }

    public List<EvaluationEvidenceRecord> evaluate(String datasetId, List<String> caseIds) {
        EvaluationDataset dataset = datasetRegistry.resolve(datasetId);
        List<String> selectedCaseIds = caseIds == null || caseIds.isEmpty()
                ? dataset.cases().stream().map(RetrievalEvaluationCase::caseId).toList()
                : List.copyOf(caseIds);
        if (selectedCaseIds.stream().anyMatch(caseId -> caseId == null || caseId.isBlank())) {
            throw new IllegalArgumentException("evaluation case id is required");
        }
        if (selectedCaseIds.stream().distinct().count() != selectedCaseIds.size()) {
            throw new IllegalArgumentException("evaluation case ids must be unique");
        }
        return selectedCaseIds.stream().map(caseId -> evaluate(datasetId, caseId)).toList();
    }

    private EvaluationEvidenceRecord toEvidence(EvaluationDataset dataset,
                                                RetrievalEvaluationCase evaluationCase,
                                                KnowledgeRetrievalResult retrievalResult,
                                                long durationMillis,
                                                EvaluationRetrievalConfiguration retrievalConfiguration) {
        KnowledgeRetrievalResult result = Objects.requireNonNull(retrievalResult, "retrieval result is required");
        KnowledgeRetrievalQualityReport qualityReport = qualityEvaluator.evaluate(List.of(
                new KnowledgeRetrievalQualityCase(evaluationCase.caseId(), evaluationCase.expectedCitationKeys(), result)));
        EvaluationFailure failure = result.outcome() == com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome.FAILURE
                ? failureOf(result)
                : null;
        return new EvaluationEvidenceRecord(
                EVIDENCE_CONTRACT_VERSION,
                dataset.datasetId(),
                dataset.datasetVersion(),
                evaluationCase.caseId(),
                result.outcome(),
                IntStream.range(0, result.citations().size()).mapToObj(index -> {
                    var citation = result.citations().get(index);
                    return new EvaluationEvidenceCitation(
                            citation.citationKey(), citation.knowledgeBaseId(), citation.documentId(), citation.versionId(),
                            citation.chunkId(), index + 1, citation.score());
                }).toList(),
                result.contexts().stream().map(context -> new EvaluationEvidenceContext(
                        context.citationKey(), context.content())).toList(),
                metadataCount(result.metadata(), "candidateCount"),
                metadataCount(result.metadata(), "finalCount"),
                durationMillis,
                retrievalConfiguration,
                new EvaluationDeterministicResult(
                        DETERMINISTIC_METRIC_VERSION,
                        qualityReport.recallAtK(),
                        qualityReport.rankingAccuracy(),
                        qualityReport.citationCoverage(),
                        qualityReport.noHitPrecision(),
                        qualityReport.thresholdsMet()),
                failure);
    }

    private EvaluationRetrievalConfiguration retrievalConfiguration() {
        return new EvaluationRetrievalConfiguration(
                ragProperties.isRetrievalEnabled(),
                ragProperties.isRerankingEnabled(),
                ragProperties.getTopK(),
                ragProperties.getCandidateLimit(),
                ragProperties.getMinimumScore(),
                ragProperties.getEmbeddingModel(),
                ragProperties.getEmbeddingDimension());
    }

    private EvaluationFailure failureOf(KnowledgeRetrievalResult result) {
        String category = result.metadata().getOrDefault("failureCategory", "UNKNOWN");
        return new EvaluationFailure(category, safeFailureMessage(category));
    }

    private String safeFailureMessage(String category) {
        return switch (category) {
            case "VECTOR_STORE" -> "Evaluation retrieval failed at the vector store.";
            case "RERANKER" -> "Evaluation retrieval failed during reranking.";
            case "AUTHORIZATION" -> "Evaluation retrieval failed during authorization.";
            default -> "Evaluation retrieval failed.";
        };
    }

    private int metadataCount(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (value == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
