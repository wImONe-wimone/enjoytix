import json

import pytest

from ragas_evaluation.contracts import join_cases, load_dataset, load_evidence
from ragas_evaluation.ragas_adapter import (
    EVALUATION_METRIC_IDS,
    EvaluatorConfiguration,
    EvaluatorError,
    RagasEvaluation,
    RagasMappingBoundary,
    evaluate_with_ragas,
)


def dataset_case() -> dict:
    return {
        "schemaVersion": "1",
        "datasetVersion": "dataset-1",
        "caseId": "case-1",
        "query": "refund policy",
        "subjectProfileId": "public-user",
        "expectedOutcome": "SUCCESS",
        "expectedCitationKeys": ["cite-1"],
        "referenceAnswer": "Refunds follow the policy.",
        "tags": ["positive"],
    }


def evidence_record() -> dict:
    return {
        "contractVersion": "1",
        "datasetId": "dataset",
        "datasetVersion": "dataset-1",
        "caseId": "case-1",
        "retrievalOutcome": "SUCCESS",
        "citations": [
            {
                "citationKey": "cite-1",
                "knowledgeBaseId": "kb-1",
                "documentId": "doc-1",
                "versionId": "v1",
                "chunkId": "chunk-1",
                "rank": 1,
                "score": 0.9,
            }
        ],
        "contexts": [{"citationKey": "cite-1", "content": "Refunds follow the policy."}],
        "candidateCount": 1,
        "finalCount": 1,
        "retrievalDurationMillis": 3,
        "retrievalConfiguration": {
            "retrievalEnabled": True,
            "rerankingEnabled": False,
            "topK": 5,
            "candidateLimit": 10,
            "minimumScore": 0.2,
            "embeddingModel": "embedding-model",
            "embeddingDimension": 1024,
        },
        "deterministicResult": {
            "metricVersion": "1",
            "recallAtK": 1.0,
            "rankingAccuracy": 1.0,
            "citationCoverage": 1.0,
            "noHitPrecision": 1.0,
            "thresholdsMet": True,
        },
        "failure": None,
    }


def joined_case(tmp_path):
    dataset_path = tmp_path / "dataset.jsonl"
    evidence_path = tmp_path / "evidence.jsonl"
    dataset_path.write_text(json.dumps(dataset_case()) + "\n", encoding="utf-8")
    evidence_path.write_text(json.dumps(evidence_record()) + "\n", encoding="utf-8")
    return join_cases(load_dataset(dataset_path), load_evidence(evidence_path))


def test_mapping_uses_pinned_retrieval_context_columns(tmp_path):
    mapped = RagasMappingBoundary().map(joined_case(tmp_path))

    assert mapped == [
        {
            "user_input": "refund policy",
            "retrieved_contexts": ["Refunds follow the policy."],
            "reference": "Refunds follow the policy.",
        }
    ]


def test_evaluator_configuration_reads_only_environment_and_redacts_secret(monkeypatch):
    monkeypatch.setenv("RAGAS_EVALUATOR_MODEL", "offline-model")
    monkeypatch.setenv("RAGAS_EVALUATOR_PROVIDER", "openai-compatible")
    monkeypatch.setenv("RAGAS_EVALUATOR_BASE_URL", "https://private.example/v1")
    monkeypatch.setenv("RAGAS_EVALUATOR_API_KEY", "super-secret")

    configuration = EvaluatorConfiguration.from_environment()

    assert configuration.model == "offline-model"
    assert configuration.identifier.startswith("openai-compatible:offline-model:")
    assert "super-secret" not in configuration.identifier
    assert "private.example" not in configuration.identifier


def test_ragas_evaluation_records_version_metric_ids_and_configuration(tmp_path, monkeypatch):
    monkeypatch.setenv("RAGAS_EVALUATOR_MODEL", "offline-model")
    configuration = EvaluatorConfiguration.from_environment()

    result = evaluate_with_ragas(
        joined_case(tmp_path),
        configuration,
        evaluator=lambda rows: {"context_precision": {"case-1": 0.8}, "context_recall": {"case-1": 0.9}},
        ragas_version_provider=lambda: "0.2.15",
    )

    assert isinstance(result, RagasEvaluation)
    assert result.ragas_version == "0.2.15"
    assert result.metric_ids == EVALUATION_METRIC_IDS
    assert result.evaluator_configuration_id == configuration.identifier
    assert result.case_scores == {
        "case-1": {"context_precision": 0.8, "context_recall": 0.9}
    }


def test_unconfigured_evaluator_is_rejected(monkeypatch, tmp_path):
    monkeypatch.delenv("RAGAS_EVALUATOR_MODEL", raising=False)

    with pytest.raises(EvaluatorError, match="RAGAS_EVALUATOR_MODEL"):
        evaluate_with_ragas(joined_case(tmp_path), EvaluatorConfiguration.from_environment())
