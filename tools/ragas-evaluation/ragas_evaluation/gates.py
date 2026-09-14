from __future__ import annotations

from dataclasses import dataclass
from typing import Sequence

from .contracts import ContractError, JoinedCase
from .ragas_adapter import EVALUATION_METRIC_IDS, EvaluatorError, RagasEvaluation


EXIT_SUCCESS = 0
EXIT_CONTRACT_ERROR = 2
EXIT_EVALUATOR_ERROR = 3
EXIT_QUALITY_GATE_FAILURE = 4


@dataclass(frozen=True)
class GateOutcome:
    passed: bool
    failed_gates: tuple[dict[str, object], ...]
    semantic_baseline_only: bool
    exit_code: int


def assess_quality_gates(
    cases: Sequence[JoinedCase],
    semantic_evaluation: RagasEvaluation,
    semantic_thresholds: dict[str, float],
) -> GateOutcome:
    thresholds = _validate_thresholds(semantic_thresholds)
    failures: list[dict[str, object]] = []
    for joined in cases:
        deterministic = joined.evidence.deterministic_result
        if not deterministic["thresholdsMet"]:
            failures.append(
                {
                    "gate": "deterministic",
                    "metric": "thresholdsMet",
                    "caseId": joined.dataset_case.case_id,
                    "reason": "deterministic retrieval thresholds were not met",
                }
            )

    for case_id, metric_scores in semantic_evaluation.case_scores.items():
        for metric_id, threshold in thresholds.items():
            score = metric_scores.get(metric_id)
            if score is None:
                raise EvaluatorError(f"Ragas evaluator did not return {metric_id} for {case_id}")
            if score < threshold:
                failures.append(
                    {
                        "gate": "semantic",
                        "metric": metric_id,
                        "caseId": case_id,
                        "score": score,
                        "threshold": threshold,
                        "reason": "semantic quality threshold was not met",
                    }
                )

    return GateOutcome(
        passed=not failures,
        failed_gates=tuple(failures),
        semantic_baseline_only=not thresholds,
        exit_code=EXIT_QUALITY_GATE_FAILURE if failures else EXIT_SUCCESS,
    )


def exit_code_for_error(error: BaseException) -> int:
    if isinstance(error, EvaluatorError):
        return EXIT_EVALUATOR_ERROR
    if isinstance(error, ContractError):
        return EXIT_CONTRACT_ERROR
    return EXIT_CONTRACT_ERROR


def _validate_thresholds(thresholds: dict[str, float]) -> dict[str, float]:
    if not isinstance(thresholds, dict):
        raise ContractError("semantic thresholds must be an object")
    unknown = sorted(set(thresholds) - set(EVALUATION_METRIC_IDS))
    if unknown:
        raise ContractError("unknown semantic metric: " + unknown[0])
    for metric_id, threshold in thresholds.items():
        if isinstance(threshold, bool) or not isinstance(threshold, (int, float)) or not 0 <= threshold <= 1:
            raise ContractError(f"semantic threshold for {metric_id} must be between 0 and 1")
    return dict(thresholds)
