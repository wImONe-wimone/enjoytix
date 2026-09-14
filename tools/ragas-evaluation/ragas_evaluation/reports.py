from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Sequence

from .contracts import JoinedCase
from .gates import GateOutcome
from .ragas_adapter import RagasEvaluation


def build_report(
    cases: Sequence[JoinedCase],
    semantic_evaluation: RagasEvaluation,
    gate_outcome: GateOutcome,
) -> dict[str, Any]:
    if not cases:
        raise ValueError("at least one case is required for a report")
    first = cases[0]
    deterministic_metrics = {
        "recallAtK": _average(item.evidence.deterministic_result["recallAtK"] for item in cases),
        "rankingAccuracy": _average(item.evidence.deterministic_result["rankingAccuracy"] for item in cases),
        "citationCoverage": _average(item.evidence.deterministic_result["citationCoverage"] for item in cases),
        "noHitPrecision": _average(item.evidence.deterministic_result["noHitPrecision"] for item in cases),
    }
    semantic_metrics = {
        metric_id: _average(
            scores[metric_id]
            for scores in semantic_evaluation.case_scores.values()
            if metric_id in scores
        )
        for metric_id in semantic_evaluation.metric_ids
    }
    failed_case_ids = {str(gate["caseId"]) for gate in gate_outcome.failed_gates if "caseId" in gate}
    case_reports = []
    for joined in cases:
        case_id = joined.dataset_case.case_id
        case_reports.append(
            {
                "caseId": case_id,
                "retrievalOutcome": joined.evidence.retrieval_outcome,
                "deterministic": {
                    "recallAtK": joined.evidence.deterministic_result["recallAtK"],
                    "rankingAccuracy": joined.evidence.deterministic_result["rankingAccuracy"],
                    "citationCoverage": joined.evidence.deterministic_result["citationCoverage"],
                    "noHitPrecision": joined.evidence.deterministic_result["noHitPrecision"],
                    "thresholdsMet": joined.evidence.deterministic_result["thresholdsMet"],
                },
                "semantic": semantic_evaluation.case_scores.get(case_id, {}),
                "status": "FAIL" if case_id in failed_case_ids else "PASS",
            }
        )

    return {
        "reportVersion": "1",
        "dataset": {
            "id": first.evidence.dataset_id,
            "version": first.evidence.dataset_version,
            "schemaVersion": first.dataset_case.schema_version,
        },
        "evidenceContractVersion": first.evidence.contract_version,
        "run": {
            "ragasVersion": semantic_evaluation.ragas_version,
            "metricIds": list(semantic_evaluation.metric_ids),
            "evaluatorConfigurationId": semantic_evaluation.evaluator_configuration_id,
            "caseCount": len(cases),
            "semanticBaselineOnly": gate_outcome.semantic_baseline_only,
        },
        "aggregateMetrics": {
            "deterministic": deterministic_metrics,
            "semantic": semantic_metrics,
        },
        "cases": case_reports,
        "failedGates": list(gate_outcome.failed_gates),
        "gateOutcome": "PASS" if gate_outcome.passed else "FAIL",
    }


def write_reports(output_dir: str | Path, report: dict[str, Any]) -> dict[str, Path]:
    destination = Path(output_dir)
    destination.mkdir(parents=True, exist_ok=True)
    json_path = destination / "report.json"
    markdown_path = destination / "report.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    markdown_path.write_text(render_markdown(report), encoding="utf-8")
    return {"json": json_path, "markdown": markdown_path}


def render_markdown(report: dict[str, Any]) -> str:
    dataset = report["dataset"]
    run = report["run"]
    aggregates = report["aggregateMetrics"]
    lines = [
        "# Retrieval Evaluation Report",
        "",
        f"- Dataset: {dataset['id']}",
        f"- Dataset version: {dataset['version']}",
        f"- Evidence contract: {report['evidenceContractVersion']}",
        f"- Ragas version: {run['ragasVersion']}",
        f"- Evaluator configuration: {run['evaluatorConfigurationId']}",
        f"- Gate outcome: **{report['gateOutcome']}**",
        "",
        "## Aggregate Metrics",
        "",
        "| Group | Metric | Value |",
        "| --- | --- | ---: |",
    ]
    for group, metrics in aggregates.items():
        for metric_id, value in metrics.items():
            value_text = "n/a" if value is None else f"{value:.4f}"
            lines.append(f"| {group} | {metric_id} | {value_text} |")
    lines.extend(["", "## Cases", "", "| Case | Retrieval outcome | Status |", "| --- | --- | --- |"])
    for case in report["cases"]:
        lines.append(f"| {case['caseId']} | {case['retrievalOutcome']} | **{case['status']}** |")
    lines.extend(["", "## Failed Gates", ""])
    if report["failedGates"]:
        for gate in report["failedGates"]:
            lines.append(f"- {gate.get('gate', 'unknown')} {gate.get('metric', 'unknown')} for case {gate.get('caseId', 'unknown')}")
    else:
        lines.append("- None")
    lines.append("")
    return "\n".join(lines)


def _average(values) -> float | None:
    collected = list(values)
    return None if not collected else sum(collected) / len(collected)
