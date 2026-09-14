import json
from pathlib import Path

import pytest

from ragas_evaluation.contracts import ContractError, join_cases, load_dataset, load_evidence


def dataset_case(case_id: str = "case-1") -> dict:
    return {
        "schemaVersion": "1",
        "datasetVersion": "dataset-1",
        "caseId": case_id,
        "query": "refund policy",
        "subjectProfileId": "public-user",
        "expectedOutcome": "SUCCESS",
        "expectedCitationKeys": ["cite-1"],
        "referenceAnswer": "Refunds follow the policy.",
        "tags": ["positive"],
    }


def evidence_record(case_id: str = "case-1", dataset_version: str = "dataset-1") -> dict:
    return {
        "contractVersion": "1",
        "datasetId": "dataset",
        "datasetVersion": dataset_version,
        "caseId": case_id,
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


def write_jsonl(path: Path, records: list[dict]) -> Path:
    path.write_text("".join(json.dumps(record) + "\n" for record in records), encoding="utf-8")
    return path


def test_join_rejects_missing_evidence_case(tmp_path):
    dataset_path = write_jsonl(tmp_path / "dataset.jsonl", [dataset_case(), dataset_case("case-2")])
    evidence_path = write_jsonl(tmp_path / "evidence.jsonl", [evidence_record()])

    dataset = load_dataset(dataset_path)
    evidence = load_evidence(evidence_path)

    with pytest.raises(ContractError, match="missing evidence cases"):
        join_cases(dataset, evidence)


def test_evidence_loader_rejects_duplicate_case_records(tmp_path):
    evidence_path = write_jsonl(tmp_path / "evidence.jsonl", [evidence_record(), evidence_record()])

    with pytest.raises(ContractError, match="duplicate evidence case"):
        load_evidence(evidence_path)


def test_join_rejects_dataset_version_mismatch(tmp_path):
    dataset = load_dataset(write_jsonl(tmp_path / "dataset.jsonl", [dataset_case()]))
    evidence = load_evidence(write_jsonl(tmp_path / "evidence.jsonl", [evidence_record(dataset_version="dataset-2")]))

    with pytest.raises(ContractError, match="dataset version"):
        join_cases(dataset, evidence)


def test_join_rejects_dataset_id_mismatch(tmp_path):
    dataset = load_dataset(write_jsonl(tmp_path / "dataset.jsonl", [dataset_case()]))
    record = evidence_record()
    record["datasetId"] = "other-dataset"
    evidence = load_evidence(write_jsonl(tmp_path / "evidence.jsonl", [record]))

    with pytest.raises(ContractError, match="dataset id"):
        join_cases(dataset, evidence)


def test_evidence_loader_rejects_unknown_contract_version(tmp_path):
    record = evidence_record()
    record["contractVersion"] = "2"

    with pytest.raises(ContractError, match="contract version"):
        load_evidence(write_jsonl(tmp_path / "evidence.jsonl", [record]))


def test_dataset_loader_rejects_invalid_input_schema(tmp_path):
    record = dataset_case()
    del record["referenceAnswer"]

    with pytest.raises(ContractError, match="referenceAnswer"):
        load_dataset(write_jsonl(tmp_path / "dataset.jsonl", [record]))
