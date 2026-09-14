## Why

EnjoyTix currently has deterministic retrieval quality gates, but it cannot reproducibly measure whether returned context is semantically complete and relevant across representative knowledge-base cases. A separate offline Ragas pipeline is needed before RAG rollout can rely on semantic quality signals, without adding LLM evaluation calls or data exposure to the production request path.

## What Changes

- Add a versioned, repository-managed evaluation dataset contract for authorized retrieval cases, including positive, no-hit, authorization-isolation, retired-version, and near-match cases.
- Add an internal, default-disabled Java evaluation adapter that executes the real authorized retrieval path and emits a stable evidence record for each curated case.
- Add a standalone Python Ragas runner that converts evidence records into the installed Ragas dataset format, evaluates retrieval-context quality, and writes JSON and Markdown reports with a non-zero failure exit code when configured gates fail.
- Keep existing deterministic metrics as required release gates; record Ragas semantic metrics as a baseline first, then allow configured thresholds to gate CI after baselines are accepted.
- Explicitly exclude online Ragas calls, public evaluation APIs, persistent evaluation databases, trace dashboards, feedback collection, and answer/tool-use evaluation from this change.

## Capabilities

### New Capabilities
- `rag-quality-evaluation`: Defines curated retrieval evaluation datasets, isolated evidence export, offline Ragas execution, and auditable quality reports.

### Modified Capabilities
- None.

## Impact

- Affected code: `services/agent-service` retrieval configuration and internal evaluation adapter, plus focused unit/integration tests.
- New repository assets: versioned evaluation JSONL data, a Python evaluation runner with locked dependencies, and generated-but-untracked evaluation reports.
- Operations: CI or a controlled local environment needs a configured evaluator-model provider; production Agent traffic, rollout policy, authorization rules, tool permissions, and purchase behavior remain unchanged.
