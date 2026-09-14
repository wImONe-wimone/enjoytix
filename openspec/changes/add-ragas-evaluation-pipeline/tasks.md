## 1. Evaluation Contract and Configuration

- [x] 1.1 Add default-disabled `agent.knowledge.evaluation` configuration, an explicit evaluation profile, and restricted internal-operation registration; verify configuration-binding tests confirm normal Agent startup exposes no evaluation operation.
- [x] 1.2 Define versioned Java models for dataset cases, registered evaluation-subject profiles, expected retrieval outcomes, deterministic results, and canonical evidence records; verify unit tests validate required IDs, schema versions, positive reference answers, and no-hit citation rules.
- [x] 1.3 Implement the versioned JSONL dataset loader and server-side registry so only approved dataset and case IDs can be selected; verify malformed JSONL, unknown subjects, duplicate IDs, and invalid no-hit cases are rejected before retrieval.
- [x] 1.4 Add a minimal source-controlled baseline dataset covering successful retrieval, no-hit, unauthorized-scope, retired-version, and near-match cases; verify the loader accepts every fixture and each expected outcome is represented.

## 2. Authorized Java Evidence Adapter

- [x] 2.1 Implement the evaluation adapter around `AuthorizedKnowledgeRetrievalService` to resolve subjects server-side and emit ordered citations, retrieved contexts, source versions, retrieval configuration, timing/count metadata, deterministic metrics, and classified failures; verify focused service tests exercise the real authorization-aware retrieval interface.
- [x] 2.2 Add the profile-gated `/internal/agent/**` evaluation operation that accepts only registered dataset and case identifiers and requires trusted internal credentials; verify web-layer tests confirm it is unavailable by default and rejects query, identity, scope, document, and other caller-supplied overrides without invoking retrieval.
- [x] 2.3 Preserve `KnowledgeRetrievalQualityEvaluator` deterministic thresholds as mandatory gate inputs for every emitted case; verify tests cover recall-at-K, first-rank accuracy, citation coverage, and no-hit precision failures.
- [x] 2.4 Apply bounded logging and response/report redaction for the adapter, excluding credentials, stack traces, storage paths, and raw retrieval data from operational surfaces; verify sanitization tests assert prohibited values cannot appear in summaries or classified errors.

## 3. Offline Ragas Runner

- [x] 3.1 Create the isolated `tools/ragas-evaluation` Python project with documented supported Python version, exactly pinned dependencies, and a single CLI entry point; verify a clean environment installs the lockfile and displays CLI help.
- [x] 3.2 Implement dataset and evidence-contract validation plus versioned case joins in the runner; verify fixture tests fail non-zero for missing cases, duplicate records, contract/dataset version mismatches, and invalid input schemas.
- [x] 3.3 Implement the Ragas mapping boundary for the pinned release's retrieval-context precision and recall metrics, with evaluator configuration provided only through environment variables; verify mocked-runner tests record the installed Ragas version, concrete metric IDs, and a non-secret evaluator configuration identifier.
- [x] 3.4 Merge Ragas results with Java deterministic results and implement baseline-versus-threshold gate behavior with distinct non-zero outcomes for contract/configuration errors, evaluator failures, and quality-gate failures; verify tests cover unset thresholds, failed semantic thresholds, failed deterministic thresholds, and unavailable evaluators.
- [x] 3.5 Generate sanitized `report.json` and `report.md` with run versions, aggregate metrics, per-case status, and failed-gate details while omitting queries, contexts, reference answers, credentials, and storage locations; verify golden-report tests and redaction assertions pass.

## 4. Operational Integration and Verification

- [x] 4.1 Ignore generated raw evidence and local evaluation outputs while allowing only approved sanitized report handling; verify `git check-ignore` covers the generated paths and source-controlled fixtures remain tracked.
- [x] 4.2 Document controlled evaluation setup, required evaluator environment variables, adapter invocation, baseline review, threshold promotion, failure exit codes, and raw-evidence handling; verify the documented commands run against the committed fixtures without exposing secrets.
- [x] 4.3 Add CI automation that initially publishes restricted sanitized baseline reports without enforcing semantic thresholds, then supports explicit threshold configuration for required gating; verify CI configuration runs the deterministic and offline runner commands with raw evidence unpublished.
- [x] 4.4 Run focused Java tests, Python runner tests, and a controlled end-to-end fixture evaluation; verify successful baseline output is produced and each intentionally failing gate returns the documented non-zero exit status.
