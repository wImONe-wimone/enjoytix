## Context

See `proposal.md` for motivation. `agent-service` already provides permission-aware retrieval through `AuthorizedKnowledgeRetrievalService`, preserving citation provenance and retrieved context. The current `KnowledgeRetrievalQualityEvaluator` supplies deterministic recall, ranking, citation-coverage, and no-hit gates, but it accepts in-memory cases only and has no dataset, evidence-export, or semantic-evaluation workflow.

`ragent` demonstrates a useful separation: a default-disabled evaluation endpoint returns retrieval evidence, while online trace and feedback are separate capabilities. EnjoyTix has no Python tooling convention, so the Ragas runner needs an isolated, pinned environment rather than a Maven dependency.

## Goals / Non-Goals

**Goals:**

- Run the real, permission-aware retrieval path against curated evaluation cases.
- Establish a versioned Java-to-Python evidence contract that is independent of Ragas API changes.
- Produce deterministic and semantic quality results suitable for controlled local runs and CI.
- Keep evaluation secrets, evidence content, and LLM calls out of production Agent traffic and ordinary observability.

**Non-Goals:**

- Evaluating generated answers, faithfulness, answer relevancy, Agent tool use, or purchase flows.
- Adding online Ragas calls, a public API, an evaluation database, a dashboard, trace persistence, or user feedback collection.
- Changing retrieval rollout, citation behavior, authorization, model selection, or ticketing contracts.

## Decisions

### 1. Use a server-resolved internal batch evaluation adapter

Add an `agent.knowledge.evaluation` module in `agent-service` with `enabled=false` by default and an explicit evaluation profile. Its internal operation will be mounted only under `/internal/agent/**`, excluded from public Gateway routing, and require the evaluation environment's trusted service credential.

The operation accepts a registered dataset identifier and optional case identifiers, not arbitrary queries or identities. A Java dataset loader resolves the identifier to a checked-in JSONL asset, validates the schema before retrieval, and resolves each subject-profile identifier to a server-owned `AgentUserContext`. The adapter calls `AuthorizedKnowledgeRetrievalService` directly, so authorization, active-version filtering, reranking, citation assembly, outcomes, and failure classification remain exactly those exercised by the real RAG path. It deliberately bypasses only the interactive Agent orchestration and rollout cohort decision.

This follows ragent's evidence-only evaluation pattern but strengthens the boundary for EnjoyTix: no evaluation caller can substitute a user, scope, document, or free-form query. A local Java-only test double would be easier to call but would not prove the deployed internal integration; a public endpoint would violate the Agent API boundary and create an unnecessary knowledge-exposure path.

### 2. Define a stable canonical JSONL contract before Ragas mapping

Store datasets under a dedicated repository directory, for example `evaluation/rag-retrieval/datasets/`. Each JSONL record carries `schemaVersion`, `datasetVersion`, `caseId`, `query`, `subjectProfileId`, `expectedOutcome`, `expectedCitationKeys`, `referenceAnswer`, and tags. Citation expectations use the existing opaque citation keys and therefore remain aligned with existing deterministic evaluation.

The adapter returns and can persist an untracked evidence JSONL stream with `contractVersion`, dataset/case identifiers, retrieval outcome, ordered citation metadata, ordered context text, candidate/final counts, retrieval duration, and a sanitized retrieval-configuration snapshot. The adapter also creates the existing deterministic quality report from the same cases and results. The contract does not depend on a Ragas object or column name.

Expected citation keys and reference answers have different purposes: keys support deterministic correctness gates; the human-curated answer gives the offline runner the reference material required by retrieval-context semantic metrics. Positive data is reviewed with the same access controls as the knowledge base. Evidence content is temporary and untracked; summary reports contain case IDs, citation identifiers, metrics, and failures but never query text, contexts, reference answers, credentials, or storage locations.

### 3. Isolate Ragas in a pinned Python runner

Create `tools/ragas-evaluation/` as a self-contained Python project with an exactly pinned dependency file, a documented supported Python version, and a single command-line entry point. The runner accepts a dataset path, evidence path or adapter endpoint, evaluator provider settings through environment variables, output directory, and optional semantic thresholds.

The runner validates the canonical contract, joins by dataset/case/contract versions, and maps only then to the pinned Ragas schema. It evaluates retrieval-context metrics—initially the installed release's context-precision and context-recall equivalents—and records the actual metric IDs in the report. This mapping layer is the only component that changes when Ragas changes its dataset or metric APIs. The runner does not call the Agent chat endpoint and cannot invoke tools or transactions.

Evaluator credentials and provider URLs remain environment variables, never JSONL fields, CLI output, reports, or committed configuration. The report records a non-secret evaluator configuration identifier such as a model alias and a hash of non-secret settings. Provider failures fail the evaluation rather than silently producing a passing report.

### 4. Preserve deterministic gates and introduce semantic gates in two stages

The adapter remains the source of truth for existing deterministic metrics and their `RagProperties` thresholds. The Python runner merges that result with Ragas metrics into `report.json` and `report.md`; it returns distinct non-zero outcomes for contract/configuration errors, evaluator errors, and quality-gate failures.

At first, semantic metrics are baseline-only: they are collected, trendable, and visible in reports, but no unset threshold can block a run. After a reviewed baseline is accepted, configuration can set per-metric semantic thresholds. A missing, malformed, or failed Ragas evaluation is always an evaluation failure, regardless of baseline mode. This prevents unavailable quality evidence from being mistaken for a passing gate.

### 5. Keep evaluation artifacts and observability separate from production telemetry

Add generated evidence and report directories to `.gitignore`. CI may retain the sanitized summary reports as restricted artifacts; raw evidence must remain in the controlled evaluation workspace and must not be published by default. Evaluation execution emits only bounded operational logs such as dataset ID, case count, duration, outcome, and failure category. It does not reuse production query/document telemetry or create trace/feedback tables.

## Risks / Trade-offs

- **Evaluator LLM variability and cost** → Pin the runner and metric configuration, record provider/model identifiers, establish a baseline before enforcing semantic thresholds, and run only curated datasets.
- **Evaluator provider outage or rate limit** → Fail the evaluation explicitly, preserve deterministic output for diagnosis, and never treat missing semantic data as a passing result.
- **Knowledge versions drift from the dataset** → Include expected version-bearing citation keys and dataset versions, reject mismatched evidence, and refresh/review datasets whenever active knowledge changes intentionally.
- **Internal endpoint exposes retrieval text** → Register it only in the explicit evaluation profile, require trusted service credentials, accept only registered dataset cases, and keep it outside public routes.
- **Evidence accidentally reaches source control or logs** → Keep raw evidence in ignored output directories, redact output surfaces, and add tests that assert secrets, contexts, and raw queries are absent from summaries.

## Migration Plan

1. Add the default-disabled evaluation configuration, dataset contract, adapter, and contract/unit tests without changing normal Agent routing.
2. Add a minimal curated dataset and run the adapter in a controlled evaluation environment with retrieval enabled and approved knowledge fixtures.
3. Add the pinned Python runner, run it manually and in a non-blocking CI job, and review deterministic and semantic baseline reports.
4. Configure approved semantic thresholds and promote the CI job to a required release gate.
5. Roll back by disabling `agent.knowledge.evaluation`, removing the CI invocation or semantic thresholds, and deleting temporary evidence. No production data migration, public-route change, or RAG rollout rollback is required.
