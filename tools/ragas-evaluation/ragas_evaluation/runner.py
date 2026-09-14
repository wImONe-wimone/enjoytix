from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Sequence

from .contracts import Dataset, Evidence, JoinedCase, join_cases, load_dataset, load_evidence
from .gates import GateOutcome, assess_quality_gates
from .ragas_adapter import EvaluatorConfiguration, RagasEvaluation, evaluate_with_ragas
from .reports import build_report, write_reports


@dataclass(frozen=True)
class EvaluationRun:
    dataset: Dataset
    evidence: tuple[Evidence, ...]
    cases: tuple[JoinedCase, ...]
    semantic_evaluation: RagasEvaluation
    gate_outcome: GateOutcome
    report: dict[str, Any]
    report_paths: dict[str, Path]


def run_evaluation(
    dataset_path: str | Path,
    evidence_path: str | Path,
    output_dir: str | Path,
    case_ids: Sequence[str] | None = None,
    semantic_thresholds: dict[str, float] | None = None,
    *,
    configuration: EvaluatorConfiguration | None = None,
    evaluator: Callable[[list[dict[str, Any]]], dict[str, dict[str, float]]] | None = None,
    ragas_version_provider: Callable[[], str] | None = None,
) -> EvaluationRun:
    dataset = load_dataset(dataset_path)
    evidence = load_evidence(evidence_path)
    cases = join_cases(dataset, evidence, list(case_ids) if case_ids is not None else None)
    semantic_evaluation = evaluate_with_ragas(
        cases,
        configuration=configuration,
        evaluator=evaluator,
        ragas_version_provider=ragas_version_provider,
    )
    gate_outcome = assess_quality_gates(cases, semantic_evaluation, semantic_thresholds or {})
    report = build_report(cases, semantic_evaluation, gate_outcome)
    report_paths = write_reports(output_dir, report)
    return EvaluationRun(
        dataset=dataset,
        evidence=evidence,
        cases=cases,
        semantic_evaluation=semantic_evaluation,
        gate_outcome=gate_outcome,
        report=report,
        report_paths=report_paths,
    )
