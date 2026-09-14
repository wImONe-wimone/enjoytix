## MODIFIED Requirements

### Requirement: Workflow execution is observable

Each AI run MUST expose correlation information and execution status suitable for logs, metrics, and future graph-node tracing, including whether retrieval was attempted, retrieval outcome, citation coverage, and RAG latency when the RAG path is enabled. Each run MUST also record the rollout decision, selected cohort key, and any fallback reason without recording raw document content, secrets, or authorization headers.

#### Scenario: Run completes with tool calls

- **WHEN** an assistant run completes after one or more tool calls
- **THEN** the result includes a run identifier and final status
- **AND** metrics distinguish model latency, tool latency, retrieval latency, and failure outcome

#### Scenario: Run completes with retrieved knowledge

- **WHEN** an assistant run uses one or more authorized knowledge chunks
- **THEN** the run records retrieval outcome and citation coverage under the same conversation and run identifiers
- **AND** the run records that the RAG rollout selected the request without recording retrieved document bodies

#### Scenario: Run uses the pre-RAG fallback

- **WHEN** RAG is disabled, the request is outside the cohort, rollback is active, or retrieval fails
- **THEN** the run records the selected pre-RAG path and a bounded fallback reason
- **AND** the existing run and purchase correlation identifiers remain available