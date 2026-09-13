# AI Workflow Foundation Specification

## ADDED Requirements

### Requirement: Conversation state is independent from model implementation

The AI service MUST represent conversation and run state using application-owned identifiers and DTOs so that the initial ChatClient flow can later be mapped to Spring AI Alibaba Graph nodes without changing external API contracts.

#### Scenario: Continue a conversation

- **WHEN** a valid conversation identifier is supplied
- **THEN** the service restores the permitted conversation context and continues the run
- **AND** the model provider remains an internal implementation detail

### Requirement: Workflow execution is observable

Each AI run MUST expose correlation information and execution status suitable for logs, metrics, and future graph-node tracing.

#### Scenario: Run completes with tool calls

- **WHEN** an assistant run completes after one or more tool calls
- **THEN** the result includes a run identifier and final status
- **AND** metrics distinguish model latency, tool latency, and failure outcome

### Requirement: Graph adoption is incremental

The initial release MUST use single-agent tool calling and MUST preserve explicit seams for a later Graph Workflow implementation.

#### Scenario: Graph workflow is not enabled

- **WHEN** graph mode is disabled by configuration
- **THEN** the existing single-agent flow remains the active execution path
- **AND** existing conversational and purchase contracts remain compatible
