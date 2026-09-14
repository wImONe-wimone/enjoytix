from dataclasses import replace
import json

import pytest

from ragas_evaluation.contracts import ContractError, join_cases, load_dataset, load_evidence
from ragas_evaluation.gates import (
    EXIT_CONTRACT_ERROR,
    EXIT_EVALUATOR_ERROR,
    EXIT_QUALITY_GATE_FAILURE,
    assess_quality_gates,
    exit_code_for_error,
)
from ragas_evaluation.ragas_adapter import EvaluatorError, RagasEvaluation


def joined_case(tmp_path, deterministic_passed=True):
    dataset = {
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
    evidence = {
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
            "thresholdsMet": deterministic_passed,
        },
        "failure": None,
    }
    dataset_path = tmp_path / "dataset.jsonl"
    evidence_path = tmp_path / "evidence.jsonl"
    dataset_path.write_text(json.dumps(dataset) + "\n", encoding="utf-8")
    evidence_path.write_text(json.dumps(evidence) + "\n", encoding="utf-8")
    return join_cases(load_dataset(dataset_path), load_evidence(evidence_path))


def semantic_result(score):
    return RagasEvaluation(
        "0.2.15",
        ("context_precision", "context_recall"),
        "provider:model:identifier",
        {"case-1": {"context_precision": score, "context_recall": score}},
    )


def test_unset_semantic_threshold_keeps_semantic_scores_in_baseline_mode(tmp_path):
    outcome = assess_quality_gates(joined_case(tmp_path), semantic_result(0.1), {})

    assert outcome.passed
    assert outcome.failed_gates == ()
    assert outcome.semantic_baseline_only


def test_failed_semantic_threshold_returns_quality_gate_failure(tmp_path):
    outcome = assess_quality_gates(
        joined_case(tmp_path),
        semantic_result(0.6),
        {"context_precision": 0.8},
    )

    assert not outcome.passed
    assert outcome.failed_gates[0]["metric"] == "context_precision"
    assert outcome.failed_gates[0]["caseId"] == "case-1"
    assert outcome.exit_code == EXIT_QUALITY_GATE_FAILURE


def test_failed_deterministic_threshold_returns_quality_gate_failure(tmp_path):
    outcome = assess_quality_gates(joined_case(tmp_path, deterministic_passed=False), semantic_result(1.0), {})

    assert not outcome.passed
    assert outcome.failed_gates[0]["gate"] == "deterministic"
    assert outcome.exit_code == EXIT_QUALITY_GATE_FAILURE


@pytest.mark.parametrize(
    ("error", "expected"),
    [
        (ContractError("bad contract"), EXIT_CONTRACT_ERROR),
        (EvaluatorError("evaluator unavailable"), EXIT_EVALUATOR_ERROR),
    ],
)
def test_non_quality_errors_have_distinct_exit_codes(error, expected):
    assert exit_code_for_error(error) == expected
