from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SUPPORTED_CONTRACT_VERSION = "1"
SUPPORTED_SCHEMA_VERSION = "1"


class ContractError(ValueError):
    """Raised when an evaluation dataset or evidence stream is invalid."""


@dataclass(frozen=True)
class DatasetCase:
    schema_version: str
    dataset_version: str
    case_id: str
    query: str
    subject_profile_id: str
    expected_outcome: str
    expected_citation_keys: tuple[str, ...]
    reference_answer: str | None
    tags: tuple[str, ...]


@dataclass(frozen=True)
class Dataset:
    dataset_id: str
    schema_version: str
    dataset_version: str
    cases: tuple[DatasetCase, ...]


@dataclass(frozen=True)
class Evidence:
    contract_version: str
    dataset_id: str
    dataset_version: str
    case_id: str
    retrieval_outcome: str
    citations: tuple[dict[str, Any], ...]
    contexts: tuple[dict[str, Any], ...]
    candidate_count: int
    final_count: int
    retrieval_duration_millis: int
    retrieval_configuration: dict[str, Any]
    deterministic_result: dict[str, Any]
    failure: dict[str, Any] | None


@dataclass(frozen=True)
class JoinedCase:
    dataset_case: DatasetCase
    evidence: Evidence


def load_dataset(path: str | Path, dataset_id: str | None = None) -> Dataset:
    records = _read_jsonl(path, "dataset")
    if not records:
        raise ContractError("dataset must contain at least one case")

    cases = tuple(_parse_dataset_case(record, index) for index, record in enumerate(records, 1))
    case_ids = [case.case_id for case in cases]
    if len(set(case_ids)) != len(case_ids):
        raise ContractError("duplicate dataset case")

    schema_versions = {case.schema_version for case in cases}
    dataset_versions = {case.dataset_version for case in cases}
    if schema_versions != {SUPPORTED_SCHEMA_VERSION}:
        raise ContractError("unsupported or inconsistent dataset schema version")
    if len(dataset_versions) != 1:
        raise ContractError("inconsistent dataset version")

    resolved_id = dataset_id or Path(path).stem
    if not resolved_id or resolved_id in {".", ".."}:
        raise ContractError("dataset id is required")
    return Dataset(resolved_id, cases[0].schema_version, cases[0].dataset_version, cases)


def load_evidence(path: str | Path) -> tuple[Evidence, ...]:
    records = _read_jsonl(path, "evidence")
    evidence = tuple(_parse_evidence(record, index) for index, record in enumerate(records, 1))
    case_ids = [item.case_id for item in evidence]
    if len(set(case_ids)) != len(case_ids):
        raise ContractError("duplicate evidence case")
    dataset_ids = {item.dataset_id for item in evidence}
    if len(dataset_ids) > 1:
        raise ContractError("evidence dataset ids must match")
    dataset_versions = {item.dataset_version for item in evidence}
    if len(dataset_versions) > 1:
        raise ContractError("evidence dataset versions must match")
    return evidence


def join_cases(
    dataset: Dataset,
    evidence: tuple[Evidence, ...] | list[Evidence],
    case_ids: list[str] | tuple[str, ...] | None = None,
) -> tuple[JoinedCase, ...]:
    if not isinstance(dataset, Dataset):
        raise ContractError("dataset is invalid")

    evidence_items = tuple(evidence)
    selected_ids = tuple(case_ids) if case_ids is not None else tuple(case.case_id for case in dataset.cases)
    if not selected_ids:
        raise ContractError("at least one evaluation case is required")
    if len(set(selected_ids)) != len(selected_ids):
        raise ContractError("duplicate requested case")

    dataset_by_id = {case.case_id: case for case in dataset.cases}
    evidence_by_id = {item.case_id: item for item in evidence_items}
    unknown_cases = sorted(set(evidence_by_id) - set(dataset_by_id))
    if unknown_cases:
        raise ContractError("evidence contains unknown cases")
    missing_cases = sorted(set(selected_ids) - set(evidence_by_id))
    if missing_cases:
        raise ContractError("missing evidence cases: " + ", ".join(missing_cases))
    unknown_requested = sorted(set(selected_ids) - set(dataset_by_id))
    if unknown_requested:
        raise ContractError("dataset contains no requested cases")

    for item in evidence_items:
        if item.dataset_id != dataset.dataset_id:
            raise ContractError("evidence dataset id does not match dataset id")
        if item.contract_version != dataset.schema_version:
            raise ContractError("evidence contract version does not match dataset schema version")
        if item.dataset_version != dataset.dataset_version:
            raise ContractError("evidence dataset version does not match dataset version")

    return tuple(JoinedCase(dataset_by_id[case_id], evidence_by_id[case_id]) for case_id in selected_ids)


def _read_jsonl(path: str | Path, kind: str) -> list[dict[str, Any]]:
    source = Path(path)
    try:
        lines = source.read_text(encoding="utf-8").splitlines()
    except OSError as error:
        raise ContractError(f"cannot read {kind} input") from error

    records: list[dict[str, Any]] = []
    for line_number, line in enumerate(lines, 1):
        if not line.strip():
            raise ContractError(f"{kind} line {line_number} is blank")
        try:
            value = json.loads(line)
        except json.JSONDecodeError as error:
            raise ContractError(f"invalid {kind} JSON at line {line_number}") from error
        if not isinstance(value, dict):
            raise ContractError(f"{kind} line {line_number} must be an object")
        records.append(value)
    return records


def _parse_dataset_case(record: dict[str, Any], line_number: int) -> DatasetCase:
    required = {
        "schemaVersion",
        "datasetVersion",
        "caseId",
        "query",
        "subjectProfileId",
        "expectedOutcome",
        "expectedCitationKeys",
        "referenceAnswer",
        "tags",
    }
    _require_exact_keys(record, required, "dataset", line_number)
    schema_version = _text(record["schemaVersion"], "schemaVersion")
    dataset_version = _text(record["datasetVersion"], "datasetVersion")
    case_id = _text(record["caseId"], "caseId")
    query = _text(record["query"], "query")
    subject_profile_id = _text(record["subjectProfileId"], "subjectProfileId")
    expected_outcome = _text(record["expectedOutcome"], "expectedOutcome")
    if expected_outcome not in {"SUCCESS", "NO_HIT"}:
        raise ContractError("expectedOutcome must be SUCCESS or NO_HIT")
    expected_citation_keys = _string_tuple(record["expectedCitationKeys"], "expectedCitationKeys")
    reference_answer = record["referenceAnswer"]
    if reference_answer is not None and not isinstance(reference_answer, str):
        raise ContractError("referenceAnswer must be a string or null")
    if expected_outcome == "SUCCESS" and not reference_answer:
        raise ContractError("successful case referenceAnswer is required")
    if expected_outcome == "NO_HIT" and expected_citation_keys:
        raise ContractError("no-hit case must not declare expected citations")
    if expected_outcome == "NO_HIT" and reference_answer not in (None, ""):
        raise ContractError("no-hit case referenceAnswer must be null")
    tags = _string_tuple(record["tags"], "tags")
    return DatasetCase(
        schema_version,
        dataset_version,
        case_id,
        query,
        subject_profile_id,
        expected_outcome,
        expected_citation_keys,
        reference_answer,
        tags,
    )


def _parse_evidence(record: dict[str, Any], line_number: int) -> Evidence:
    required = {
        "contractVersion",
        "datasetId",
        "datasetVersion",
        "caseId",
        "retrievalOutcome",
        "citations",
        "contexts",
        "candidateCount",
        "finalCount",
        "retrievalDurationMillis",
        "retrievalConfiguration",
        "deterministicResult",
        "failure",
    }
    _require_exact_keys(record, required, "evidence", line_number)
    contract_version = _text(record["contractVersion"], "contractVersion")
    if contract_version != SUPPORTED_CONTRACT_VERSION:
        raise ContractError("unsupported evidence contract version")
    dataset_id = _text(record["datasetId"], "datasetId")
    dataset_version = _text(record["datasetVersion"], "datasetVersion")
    case_id = _text(record["caseId"], "caseId")
    outcome = _text(record["retrievalOutcome"], "retrievalOutcome")
    if outcome not in {"SUCCESS", "NO_HIT", "FAILURE"}:
        raise ContractError("retrievalOutcome is invalid")
    citations = _parse_citations(record["citations"])
    contexts = _parse_contexts(record["contexts"])
    _validate_contexts(citations, contexts)
    candidate_count = _non_negative_int(record["candidateCount"], "candidateCount")
    final_count = _non_negative_int(record["finalCount"], "finalCount")
    if final_count > candidate_count:
        raise ContractError("finalCount must not exceed candidateCount")
    retrieval_duration = _non_negative_int(record["retrievalDurationMillis"], "retrievalDurationMillis")
    configuration = _parse_retrieval_configuration(record["retrievalConfiguration"])
    deterministic = _parse_deterministic_result(record["deterministicResult"])
    failure = record["failure"]
    if failure is not None:
        _parse_failure(failure)
    if outcome == "NO_HIT" and (citations or contexts or failure is not None):
        raise ContractError("no-hit evidence must not contain citations, contexts, or failure")
    if outcome == "FAILURE" and (citations or contexts or failure is None):
        raise ContractError("failed evidence must contain only a classified failure")
    if outcome != "FAILURE" and failure is not None:
        raise ContractError("non-failed evidence must not contain failure")
    return Evidence(
        contract_version,
        dataset_id,
        dataset_version,
        case_id,
        outcome,
        citations,
        contexts,
        candidate_count,
        final_count,
        retrieval_duration,
        configuration,
        deterministic,
        failure,
    )


def _parse_citations(value: Any) -> tuple[dict[str, Any], ...]:
    if not isinstance(value, list):
        raise ContractError("citations must be an array")
    required = {"citationKey", "knowledgeBaseId", "documentId", "versionId", "chunkId", "rank", "score"}
    result: list[dict[str, Any]] = []
    keys: set[str] = set()
    for index, citation in enumerate(value, 1):
        if not isinstance(citation, dict):
            raise ContractError("citation must be an object")
        _require_exact_keys(citation, required, "citation", index)
        citation_key = _text(citation["citationKey"], "citationKey")
        if citation_key in keys:
            raise ContractError("citation keys must be unique")
        keys.add(citation_key)
        if citation["rank"] != index:
            raise ContractError("citation ranks must be sequential")
        score = citation["score"]
        if isinstance(score, bool) or not isinstance(score, (int, float)) or not 0 <= score <= 1:
            raise ContractError("citation score must be between 0 and 1")
        for field in ("knowledgeBaseId", "documentId", "versionId", "chunkId"):
            _text(citation[field], field)
        result.append(citation)
    return tuple(result)


def _parse_contexts(value: Any) -> tuple[dict[str, Any], ...]:
    if not isinstance(value, list):
        raise ContractError("contexts must be an array")
    required = {"citationKey", "content"}
    result: list[dict[str, Any]] = []
    for context in value:
        if not isinstance(context, dict):
            raise ContractError("context must be an object")
        _require_exact_keys(context, required, "context", 0)
        _text(context["citationKey"], "context citationKey")
        _text(context["content"], "context content")
        result.append(context)
    return tuple(result)


def _validate_contexts(citations: tuple[dict[str, Any], ...], contexts: tuple[dict[str, Any], ...]) -> None:
    citation_keys = {citation["citationKey"] for citation in citations}
    if any(context["citationKey"] not in citation_keys for context in contexts):
        raise ContractError("context citationKey must reference a citation")


def _parse_retrieval_configuration(value: Any) -> dict[str, Any]:
    required = {
        "retrievalEnabled",
        "rerankingEnabled",
        "topK",
        "candidateLimit",
        "minimumScore",
        "embeddingModel",
        "embeddingDimension",
    }
    if not isinstance(value, dict):
        raise ContractError("retrievalConfiguration must be an object")
    _require_exact_keys(value, required, "retrievalConfiguration", 0)
    if not isinstance(value["retrievalEnabled"], bool) or not isinstance(value["rerankingEnabled"], bool):
        raise ContractError("retrieval configuration flags must be boolean")
    top_k = _positive_int(value["topK"], "topK")
    candidate_limit = _positive_int(value["candidateLimit"], "candidateLimit")
    if candidate_limit < top_k:
        raise ContractError("candidateLimit must not be less than topK")
    minimum_score = value["minimumScore"]
    if isinstance(minimum_score, bool) or not isinstance(minimum_score, (int, float)) or not 0 <= minimum_score <= 1:
        raise ContractError("minimumScore must be between 0 and 1")
    _text(value["embeddingModel"], "embeddingModel")
    _positive_int(value["embeddingDimension"], "embeddingDimension")
    return value


def _parse_deterministic_result(value: Any) -> dict[str, Any]:
    required = {
        "metricVersion",
        "recallAtK",
        "rankingAccuracy",
        "citationCoverage",
        "noHitPrecision",
        "thresholdsMet",
    }
    if not isinstance(value, dict):
        raise ContractError("deterministicResult must be an object")
    _require_exact_keys(value, required, "deterministicResult", 0)
    _text(value["metricVersion"], "metricVersion")
    for field in ("recallAtK", "rankingAccuracy", "citationCoverage", "noHitPrecision"):
        score = value[field]
        if isinstance(score, bool) or not isinstance(score, (int, float)) or not 0 <= score <= 1:
            raise ContractError(f"{field} must be between 0 and 1")
    if not isinstance(value["thresholdsMet"], bool):
        raise ContractError("thresholdsMet must be boolean")
    return value


def _parse_failure(value: Any) -> None:
    if not isinstance(value, dict):
        raise ContractError("failure must be an object or null")
    _require_exact_keys(value, {"category", "safeMessage"}, "failure", 0)
    _text(value["category"], "failure category")
    _text(value["safeMessage"], "failure safeMessage")


def _require_exact_keys(record: dict[str, Any], required: set[str], kind: str, line_number: int) -> None:
    actual = set(record)
    missing = required - actual
    extra = actual - required
    if missing:
        raise ContractError(f"{kind} is missing required field: {sorted(missing)[0]}")
    if extra:
        raise ContractError(f"{kind} contains unknown field: {sorted(extra)[0]}")


def _text(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ContractError(f"{field} must be a non-empty string")
    return value


def _string_tuple(value: Any, field: str) -> tuple[str, ...]:
    if not isinstance(value, list) or any(not isinstance(item, str) or not item.strip() for item in value):
        raise ContractError(f"{field} must be an array of non-empty strings")
    if len(set(value)) != len(value):
        raise ContractError(f"{field} must not contain duplicates")
    return tuple(value)


def _non_negative_int(value: Any, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value < 0:
        raise ContractError(f"{field} must be a non-negative integer")
    return value


def _positive_int(value: Any, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise ContractError(f"{field} must be a positive integer")
    return value
