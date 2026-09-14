# EnjoyTix Ragas Evaluation

This directory contains the isolated offline retrieval-context evaluator. It supports Python 3.10 and never calls the Java Agent chat endpoint.

## Setup

Run commands from this directory:

```text
python -m venv .venv
.venv\Scripts\python -m pip install --requirement requirements.lock
.venv\Scripts\python -m ragas_evaluation --help
```

The project pins `ragas==0.2.15` and the OpenAI-compatible LangChain adapter in `requirements.lock`. The CLI entry point is `ragas-evaluate`.

The lock file contains exact pins for the complete Python 3.10 environment, including the test runner used by CI. Install the project after the lock file so the `ragas-evaluate` console entry point is available.

## Inputs and outputs

The runner consumes a versioned dataset JSONL and matching Java evidence JSONL. Raw evidence should be written under `raw-evidence/`, which is ignored by Git. Reports contain only case IDs, versions, metric values, gate results, and classified failures; query text, contexts, reference answers, credentials, and storage locations are excluded.

```text
python -m ragas_evaluation \
  --dataset ..\path\to\baseline.jsonl \
  --evidence raw-evidence\baseline.jsonl \
  --output-dir reports
```

Use `--case-id CASE_ID` repeatedly to run a subset. Use `--semantic-threshold context_precision=0.80` or `--semantic-threshold context_recall=0.80` to promote a baseline metric to a gate.

## Evaluator configuration

Evaluator credentials and provider settings are environment variables only:

- `RAGAS_EVALUATOR_MODE`: `ragas` for the pinned evaluator or `mock` for offline deterministic checks.
- `RAGAS_EVALUATOR_MODEL`: required model alias.
- `RAGAS_EVALUATOR_PROVIDER`: non-secret provider label used in the report identifier.
- `RAGAS_EVALUATOR_BASE_URL`: optional OpenAI-compatible endpoint.
- `RAGAS_EVALUATOR_API_KEY`: required for real evaluator mode; never place it in JSONL or reports.

For a local contract and gate smoke test:

```text
$env:RAGAS_EVALUATOR_MODE = 'mock'
$env:RAGAS_EVALUATOR_MODEL = 'offline-model'
$env:RAGAS_MOCK_CONTEXT_PRECISION = '1.0'
$env:RAGAS_MOCK_CONTEXT_RECALL = '1.0'
python -m ragas_evaluation --dataset dataset.jsonl --evidence raw-evidence\evidence.jsonl --output-dir reports
```

## Java adapter

The Java adapter is disabled by default and is available only with the `evaluation` Spring profile, `AGENT_KNOWLEDGE_EVALUATION_ENABLED=true`, and a trusted internal token. Configure approved subject profiles server-side, then request only registered dataset and case IDs:

Start the service in a controlled evaluation environment, for example:

```text
$env:SPRING_PROFILES_ACTIVE = 'evaluation'
$env:AGENT_KNOWLEDGE_EVALUATION_ENABLED = 'true'
$env:AGENT_KNOWLEDGE_EVALUATION_TRUSTED_TOKEN = '<token>'
```

```text
curl -X POST http://localhost:9070/internal/agent/evaluation/retrieval `
  -H 'Content-Type: application/json' `
  -H 'X-Agent-Evaluation-Token: <token>' `
  --data '{"datasetId":"baseline","caseIds":["positive-refund-policy"]}'
```

Store the response as temporary raw evidence in the controlled workspace and pass that file to the Python runner. Do not upload or commit raw evidence.

## Exit codes and promotion

- `0`: evaluation passed; semantic metrics are baseline-only when no threshold is configured.
- `2`: invalid CLI contract, dataset, evidence, or threshold configuration.
- `3`: evaluator configuration, dependency, provider, or result failure.
- `4`: deterministic or configured semantic quality gate failed.

Review the sanitized baseline report before adding thresholds to CI. CI uploads only the sanitized `report.json` and `report.md`; raw evidence remains temporary and unpublished.

Threshold promotion is explicit: first run without `--semantic-threshold` and review the baseline, then add one or more reviewed `metric=value` thresholds to the CI dispatch input or an equivalent controlled command. A threshold failure returns `4` and must block promotion until the baseline or threshold is deliberately revised.
