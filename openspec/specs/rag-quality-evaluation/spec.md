# rag-quality-evaluation Specification

## Purpose

Provides repeatable, access-controlled offline evaluation of EnjoyTix knowledge retrieval quality, combining existing deterministic gates with Ragas semantic context metrics without adding evaluator traffic to production Agent requests.

## Requirements

### Requirement: Retrieval evaluation datasets are versioned and complete
The system MUST store curated retrieval evaluation datasets as versioned JSONL assets under source control. Every case MUST have a stable case identifier, a query, an evaluation-subject profile reference, an expected retrieval outcome, expected citation keys, and dataset/schema version information. A case expecting successful retrieval MUST include a non-empty human-curated reference answer; a case expecting no hit MUST have no expected citation keys.

#### Scenario: Valid positive retrieval case
- **WHEN** a dataset case expects a successful retrieval
- **THEN** the case provides a stable identifier, an approved subject profile, expected citation keys, and a reference answer usable by semantic evaluation

#### Scenario: Invalid no-hit case is rejected
- **WHEN** a dataset case declares a no-hit outcome but includes expected citation keys
- **THEN** the evaluation operation rejects the dataset before executing retrieval

#### Scenario: Dataset covers retrieval safety boundaries
- **WHEN** the baseline evaluation dataset is maintained
- **THEN** it includes successful retrieval, no-hit, unauthorized-scope, retired-version, and near-match cases

### Requirement: Evaluation evidence uses the authorized retrieval path
The system MUST provide a default-disabled internal evaluation operation that executes only registered dataset cases through the same authorization and retrieval behavior used by the Agent RAG path. The operation MUST resolve evaluation-subject profiles on the server and MUST NOT accept caller-supplied user identities, knowledge-base scopes, document identifiers, or free-form queries. For each evaluated case, it MUST emit a versioned evidence record containing the retrieval outcome, ordered citations and retrieved contexts, source version identifiers, relevant retrieval configuration, and timing/count metadata; failures MUST be classified without exposing credentials, stack traces, or storage paths.

#### Scenario: Curated case produces retrieval evidence
- **WHEN** a trusted evaluation caller requests an enabled registered dataset case
- **THEN** the system evaluates the case using its server-resolved subject profile and returns the ordered evidence from authorized retrieval

#### Scenario: Evaluation mode is disabled
- **WHEN** evaluation mode has not been explicitly enabled in the evaluation environment
- **THEN** the internal evaluation operation is unavailable and no retrieval evidence is exposed

#### Scenario: Caller attempts to override evaluation identity
- **WHEN** a request includes a user identity or knowledge-base scope override
- **THEN** the system rejects the request and does not execute retrieval

### Requirement: Ragas runs only as offline retrieval-context evaluation
The system MUST provide an independently runnable evaluation tool that consumes the versioned dataset and evidence contract, adapts them to the pinned Ragas release, and evaluates retrieval-context quality. Ragas execution MUST occur outside the Java Agent service and MUST NOT run on a production user request, alter retrieval results, invoke Agent tools, or create ticketing transactions.

#### Scenario: Offline runner evaluates retrieved context
- **WHEN** the runner receives a valid dataset and matching evidence records
- **THEN** it evaluates the configured retrieval-context metrics and records the Ragas version, evaluator model configuration identifier, and metric results

#### Scenario: Ragas interface changes between releases
- **WHEN** the pinned Ragas release requires a different input schema or metric API
- **THEN** only the offline runner adapts the stable evidence contract and the Java evaluation operation remains unchanged

### Requirement: Evaluation reports enforce configured quality gates
The system MUST produce machine-readable and human-readable evaluation reports that identify the dataset version, evidence contract version, run configuration, aggregate metrics, per-case status, and gate outcome without including secrets or retrieved document text. Existing deterministic retrieval metrics for recall at K, first-rank accuracy, citation coverage, and no-hit precision MUST remain mandatory gates. Ragas semantic metrics MUST be reported as a baseline when no semantic threshold is configured and MUST fail the evaluation when a configured threshold is not met. The runner MUST return a non-zero exit status for invalid datasets, incomplete or mismatched evidence, evaluator failures, or any configured gate failure.

#### Scenario: Semantic metrics are in baseline mode
- **WHEN** an evaluation run has no configured semantic threshold
- **THEN** the report includes the Ragas metric values and the deterministic gate outcome without failing solely because a semantic score is below an unset threshold

#### Scenario: Configured quality gate fails
- **WHEN** a deterministic threshold or configured Ragas semantic threshold is not met
- **THEN** the report identifies the failed metric and the runner exits with a non-zero status

#### Scenario: Evidence does not match the dataset
- **WHEN** evidence is missing a requested case or has a different dataset or contract version
- **THEN** the runner marks the run invalid, produces no passing gate outcome, and exits with a non-zero status
