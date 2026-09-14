import json

from ragas_evaluation.cli import main


def write_inputs(tmp_path, deterministic_passed=True):
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
    return dataset_path, evidence_path


def configure_mock_evaluator(monkeypatch, score="1.0"):
    monkeypatch.setenv("RAGAS_EVALUATOR_MODE", "mock")
    monkeypatch.setenv("RAGAS_EVALUATOR_MODEL", "offline-model")
    monkeypatch.setenv("RAGAS_MOCK_CONTEXT_PRECISION", score)
    monkeypatch.setenv("RAGAS_MOCK_CONTEXT_RECALL", score)


def test_cli_runs_controlled_fixture_and_writes_sanitized_reports(tmp_path, monkeypatch):
    dataset_path, evidence_path = write_inputs(tmp_path)
    configure_mock_evaluator(monkeypatch)
    output_dir = tmp_path / "reports"

    exit_code = main(
        [
            "--dataset",
            str(dataset_path),
            "--evidence",
            str(evidence_path),
            "--output-dir",
            str(output_dir),
        ]
    )

    assert exit_code == 0
    assert json.loads((output_dir / "report.json").read_text(encoding="utf-8"))["gateOutcome"] == "PASS"
    assert (output_dir / "report.md").exists()


def test_cli_returns_quality_gate_failure_for_semantic_threshold(tmp_path, monkeypatch):
    dataset_path, evidence_path = write_inputs(tmp_path)
    configure_mock_evaluator(monkeypatch, score="0.6")

    exit_code = main(
        [
            "--dataset",
            str(dataset_path),
            "--evidence",
            str(evidence_path),
            "--output-dir",
            str(tmp_path / "reports"),
            "--semantic-threshold",
            "context_precision=0.8",
        ]
    )

    assert exit_code == 4


def test_cli_returns_deterministic_quality_gate_failure(tmp_path, monkeypatch):
    dataset_path, evidence_path = write_inputs(tmp_path, deterministic_passed=False)
    configure_mock_evaluator(monkeypatch)

    exit_code = main(
        ["--dataset", str(dataset_path), "--evidence", str(evidence_path), "--output-dir", str(tmp_path / "reports")]
    )

    assert exit_code == 4


def test_cli_returns_evaluator_error_when_provider_is_unavailable(tmp_path, monkeypatch):
    dataset_path, evidence_path = write_inputs(tmp_path)
    monkeypatch.setenv("RAGAS_EVALUATOR_MODE", "ragas")
    monkeypatch.setenv("RAGAS_EVALUATOR_MODEL", "offline-model")
    monkeypatch.delenv("RAGAS_EVALUATOR_API_KEY", raising=False)

    exit_code = main(
        ["--dataset", str(dataset_path), "--evidence", str(evidence_path), "--output-dir", str(tmp_path / "reports")]
    )

    assert exit_code == 3


def test_cli_returns_contract_error_for_invalid_evidence(tmp_path, monkeypatch):
    dataset_path, evidence_path = write_inputs(tmp_path)
    evidence_path.write_text("not-json\n", encoding="utf-8")
    configure_mock_evaluator(monkeypatch)

    exit_code = main(
        ["--dataset", str(dataset_path), "--evidence", str(evidence_path), "--output-dir", str(tmp_path / "reports")]
    )

    assert exit_code == 2
