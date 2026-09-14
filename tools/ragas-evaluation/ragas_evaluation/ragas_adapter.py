from __future__ import annotations

import hashlib
import importlib.metadata
import os
from dataclasses import dataclass
from typing import Any, Callable, Mapping, Sequence

from .contracts import JoinedCase


EVALUATION_METRIC_IDS = ("context_precision", "context_recall")
PINNED_RAGAS_VERSION = "0.2.15"


class EvaluatorError(RuntimeError):
    """Raised when the semantic evaluator cannot produce valid results."""


@dataclass(frozen=True)
class EvaluatorConfiguration:
    model: str
    provider: str
    base_url: str | None
    api_key: str | None
    mode: str
    identifier: str

    @classmethod
    def from_environment(cls, environment: Mapping[str, str] | None = None) -> "EvaluatorConfiguration":
        values = os.environ if environment is None else environment
        model = values.get("RAGAS_EVALUATOR_MODEL", "").strip()
        if not model:
            raise EvaluatorError("RAGAS_EVALUATOR_MODEL is required")
        provider = values.get("RAGAS_EVALUATOR_PROVIDER", "openai-compatible").strip()
        base_url = values.get("RAGAS_EVALUATOR_BASE_URL", "").strip() or None
        api_key = values.get("RAGAS_EVALUATOR_API_KEY", "").strip() or None
        mode = values.get("RAGAS_EVALUATOR_MODE", "ragas").strip().lower() or "ragas"
        if mode not in {"ragas", "mock"}:
            raise EvaluatorError("RAGAS_EVALUATOR_MODE must be ragas or mock")
        fingerprint = hashlib.sha256(
            "\0".join((provider, model, base_url or "", mode)).encode("utf-8")
        ).hexdigest()[:16]
        identifier = f"{provider}:{model}:{fingerprint}"
        return cls(model, provider, base_url, api_key, mode, identifier)


@dataclass(frozen=True)
class RagasEvaluation:
    ragas_version: str
    metric_ids: tuple[str, ...]
    evaluator_configuration_id: str
    case_scores: dict[str, dict[str, float]]


class RagasMappingBoundary:
    def map(self, cases: Sequence[JoinedCase]) -> list[dict[str, Any]]:
        rows: list[dict[str, Any]] = []
        for joined in cases:
            if joined.evidence.retrieval_outcome != "SUCCESS":
                continue
            reference = joined.dataset_case.reference_answer
            if not reference:
                raise EvaluatorError(f"case {joined.dataset_case.case_id} has no reference answer")
            rows.append(
                {
                    "user_input": joined.dataset_case.query,
                    "retrieved_contexts": [context["content"] for context in joined.evidence.contexts],
                    "reference": reference,
                }
            )
        return rows


def evaluate_with_ragas(
    cases: Sequence[JoinedCase],
    configuration: EvaluatorConfiguration | None = None,
    evaluator: Callable[[list[dict[str, Any]]], dict[str, dict[str, float]]] | None = None,
    ragas_version_provider: Callable[[], str] | None = None,
) -> RagasEvaluation:
    resolved_configuration = configuration or EvaluatorConfiguration.from_environment()
    mapped_cases = RagasMappingBoundary().map(cases)
    case_ids = [
        joined.dataset_case.case_id
        for joined in cases
        if joined.evidence.retrieval_outcome == "SUCCESS"
    ]
    if not mapped_cases:
        return RagasEvaluation(
            _installed_ragas_version(ragas_version_provider, resolved_configuration.mode == "mock"),
            EVALUATION_METRIC_IDS,
            resolved_configuration.identifier,
            {},
        )
    score_provider = evaluator or _installed_ragas_evaluator(resolved_configuration, case_ids)
    try:
        raw_scores = score_provider(mapped_cases)
    except EvaluatorError:
        raise
    except Exception as error:
        raise EvaluatorError("Ragas evaluator failed") from error
    _validate_scores(raw_scores, case_ids)
    return RagasEvaluation(
        _installed_ragas_version(ragas_version_provider, resolved_configuration.mode == "mock"),
        EVALUATION_METRIC_IDS,
        resolved_configuration.identifier,
        {
            case_id: {metric_id: float(raw_scores[metric_id][case_id]) for metric_id in EVALUATION_METRIC_IDS}
            for case_id in case_ids
        },
    )


def _installed_ragas_version(provider: Callable[[], str] | None, allow_missing: bool = False) -> str:
    if provider is not None:
        return provider()
    try:
        return importlib.metadata.version("ragas")
    except importlib.metadata.PackageNotFoundError as error:
        if allow_missing:
            return PINNED_RAGAS_VERSION
        raise EvaluatorError("pinned Ragas package is not installed") from error


def _installed_ragas_evaluator(
    configuration: EvaluatorConfiguration,
    case_ids: Sequence[str],
) -> Callable[[list[dict[str, Any]]], dict[str, dict[str, float]]]:
    if configuration.mode == "mock":
        try:
            precision = float(os.environ.get("RAGAS_MOCK_CONTEXT_PRECISION", "1.0"))
            recall = float(os.environ.get("RAGAS_MOCK_CONTEXT_RECALL", "1.0"))
        except ValueError as error:
            raise EvaluatorError("mock Ragas scores must be numeric") from error
        if not all(0 <= score <= 1 for score in (precision, recall)):
            raise EvaluatorError("mock Ragas scores must be between 0 and 1")

        def run_mock(rows: list[dict[str, Any]]) -> dict[str, dict[str, float]]:
            if len(rows) != len(case_ids):
                raise EvaluatorError("mock Ragas evaluator received inconsistent case count")
            return {
                "context_precision": {case_id: precision for case_id in case_ids},
                "context_recall": {case_id: recall for case_id in case_ids},
            }

        return run_mock
    if not configuration.api_key:
        raise EvaluatorError("RAGAS_EVALUATOR_API_KEY is required")

    try:
        from langchain_openai import ChatOpenAI
        from ragas import EvaluationDataset, SingleTurnSample, evaluate
        from ragas.metrics import context_precision, context_recall
    except ImportError as error:
        raise EvaluatorError("pinned Ragas evaluator dependencies are not installed") from error

    def run(rows: list[dict[str, Any]]) -> dict[str, dict[str, float]]:
        try:
            llm = ChatOpenAI(
                model=configuration.model,
                api_key=configuration.api_key,
                base_url=configuration.base_url,
            )
            dataset = EvaluationDataset(samples=[SingleTurnSample(**row) for row in rows])
            result = evaluate(
                dataset,
                metrics=[context_precision, context_recall],
                llm=llm,
                raise_exceptions=True,
                show_progress=False,
            )
            scores = {metric_id: {} for metric_id in EVALUATION_METRIC_IDS}
            for case_id, row in zip(case_ids, result.scores, strict=True):
                for metric_id in EVALUATION_METRIC_IDS:
                    scores[metric_id][case_id] = float(row[metric_id])
            return scores
        except Exception as error:
            raise EvaluatorError("Ragas evaluator failed") from error

    return run


def _validate_scores(scores: Any, case_ids: Sequence[str]) -> None:
    if not isinstance(scores, dict):
        raise EvaluatorError("Ragas evaluator returned invalid scores")
    for metric_id in EVALUATION_METRIC_IDS:
        metric_scores = scores.get(metric_id)
        if not isinstance(metric_scores, dict):
            raise EvaluatorError(f"Ragas evaluator did not return {metric_id}")
        for case_id in case_ids:
            value = metric_scores.get(case_id)
            if isinstance(value, bool) or not isinstance(value, (int, float)) or not 0 <= value <= 1:
                raise EvaluatorError(f"Ragas evaluator returned invalid {metric_id} for {case_id}")
