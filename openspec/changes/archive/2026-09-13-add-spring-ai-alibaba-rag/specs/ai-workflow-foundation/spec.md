# AI Workflow Foundation Specification

## MODIFIED Requirements

### Requirement: Workflow execution is observable

Each AI run MUST expose correlation information and execution status suitable for logs, metrics, and future graph-node tracing, including whether retrieval was attempted, retrieval outcome, citation coverage, and RAG latency when the RAG path is enabled.

#### Scenario: Run completes with tool calls

- **WHEN** an assistant run completes after one or more tool calls
- **THEN** the result includes a run identifier and final status
- **AND** metrics distinguish model latency, tool latency, retrieval latency, and failure outcome

#### Scenario: Run completes with retrieved knowledge

- **WHEN** an assistant run uses one or more authorized knowledge chunks
- **THEN** the run records retrieval outcome and citation coverage under the same conversation and run identifiers
