import argparse
import sys
from collections.abc import Sequence

from .contracts import ContractError
from .gates import exit_code_for_error
from .ragas_adapter import EVALUATION_METRIC_IDS, EvaluatorError
from .runner import run_evaluation


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog='ragas-evaluate',
        description='offline retrieval-context evaluation for EnjoyTix',
    )
    parser.add_argument('--dataset', required=True, help='versioned evaluation dataset JSONL')
    parser.add_argument('--evidence', required=True, help='canonical retrieval evidence JSONL')
    parser.add_argument('--output-dir', required=True, help='directory for sanitized reports')
    parser.add_argument(
        '--case-id',
        action='append',
        dest='case_ids',
        help='evaluate one registered case; repeat to select multiple cases',
    )
    parser.add_argument(
        '--semantic-threshold',
        action='append',
        default=[],
        metavar='METRIC=VALUE',
        help='enforce a semantic threshold; repeat for multiple metrics',
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    try:
        arguments = parser.parse_args(argv)
    except SystemExit as exit_signal:
        return int(exit_signal.code)
    try:
        semantic_thresholds = _parse_semantic_thresholds(arguments.semantic_threshold)
        evaluation_run = run_evaluation(
            arguments.dataset,
            arguments.evidence,
            arguments.output_dir,
            case_ids=arguments.case_ids,
            semantic_thresholds=semantic_thresholds,
        )
        return evaluation_run.gate_outcome.exit_code
    except (ContractError, EvaluatorError) as error:
        print(f'evaluation error: {_error_category(error)}', file=sys.stderr)
        return exit_code_for_error(error)


def _parse_semantic_thresholds(values: Sequence[str]) -> dict[str, float]:
    thresholds: dict[str, float] = {}
    for value in values:
        if value.count('=') != 1:
            raise ContractError('semantic threshold must use metric=value')
        metric_id, raw_threshold = (part.strip() for part in value.split('=', 1))
        if metric_id not in EVALUATION_METRIC_IDS:
            raise ContractError('unknown semantic metric')
        if metric_id in thresholds:
            raise ContractError('duplicate semantic threshold')
        try:
            threshold = float(raw_threshold)
        except ValueError as error:
            raise ContractError('semantic threshold must be numeric') from error
        if not 0 <= threshold <= 1:
            raise ContractError('semantic threshold must be between 0 and 1')
        thresholds[metric_id] = threshold
    return thresholds


def _error_category(error: BaseException) -> str:
    if isinstance(error, EvaluatorError):
        return 'evaluator'
    if isinstance(error, ContractError):
        return 'contract'
    return 'unknown'
