## MODIFIED Requirements

### Requirement: RAG rollout is observable

The service MUST expose feature-flagged RAG execution with metrics for retrieval latency, result counts, citation coverage, model latency, token usage, and estimated cost, while preserving correlation identifiers. The runtime decision MUST be stable for the same authenticated user and cohort key, and an explicit rollback control MUST override all positive rollout selections.

#### Scenario: Gray rollout observation

- **WHEN** RAG is enabled for a configured cohort
- **THEN** each run records whether retrieval was attempted, its outcome, citation coverage, latency, and cost dimensions using the conversation and run identifiers
- **AND** the run records the cohort decision without exposing the user's raw query or document content

#### Scenario: Non-cohort fallback observation

- **WHEN** a request is outside the configured internal or canary cohort
- **THEN** retrieval is not attempted and the run records a pre-RAG fallback reason under the same correlation identifiers

#### Scenario: Explicit rollback observation

- **WHEN** the rollback control is active
- **THEN** retrieval, context injection, and citation emission are disabled for every cohort
- **AND** the run records rollback as the bounded fallback reason