import json

from test_gates import joined_case, semantic_result

from ragas_evaluation.gates import assess_quality_gates
from ragas_evaluation.reports import build_report, write_reports


def test_report_contains_auditable_metrics_without_retrieval_content(tmp_path):
    cases = joined_case(tmp_path)
    semantic = semantic_result(0.8)
    gates = assess_quality_gates(cases, semantic, {})

    report = build_report(cases, semantic, gates)
    serialized = json.dumps(report, ensure_ascii=False)

    assert report["dataset"]["version"] == "dataset-1"
    assert report["evidenceContractVersion"] == "1"
    assert report["run"]["ragasVersion"] == "0.2.15"
    assert report["run"]["metricIds"] == ["context_precision", "context_recall"]
    assert report["cases"] == [
        {
            "caseId": "case-1",
            "retrievalOutcome": "SUCCESS",
            "deterministic": {
                "recallAtK": 1.0,
                "rankingAccuracy": 1.0,
                "citationCoverage": 1.0,
                "noHitPrecision": 1.0,
                "thresholdsMet": True,
            },
            "semantic": {"context_precision": 0.8, "context_recall": 0.8},
            "status": "PASS",
        }
    ]
    assert "refund policy" not in serialized
    assert "Refunds follow the policy." not in serialized
    assert "super-secret" not in serialized
    assert "C:\\private" not in serialized


def test_report_files_are_json_and_markdown_summaries_only(tmp_path):
    cases = joined_case(tmp_path)
    semantic = semantic_result(0.8)
    gates = assess_quality_gates(cases, semantic, {})

    paths = write_reports(tmp_path / "reports", build_report(cases, semantic, gates))

    assert paths["json"].name == "report.json"
    assert paths["markdown"].name == "report.md"
    assert json.loads(paths["json"].read_text(encoding="utf-8"))["gateOutcome"] == "PASS"
    markdown = paths["markdown"].read_text(encoding="utf-8")
    assert "# Retrieval Evaluation Report" in markdown
    assert "case-1" in markdown
    assert "refund policy" not in markdown
