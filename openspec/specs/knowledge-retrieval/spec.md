# Knowledge Retrieval Specification

## Purpose

Provides permission-aware, versioned, citation-preserving retrieval so the Agent assistant can answer from approved knowledge documents without treating retrieved content as executable instructions.

## Requirements

### Requirement: Knowledge retrieval enforces scope

The service MUST restrict indexing and retrieval to knowledge bases the authenticated subject is authorized to access, and MUST apply the same scope to every document version and chunk.

#### Scenario: Unauthorized knowledge base query

- **WHEN** a user requests retrieval from a knowledge base outside their permitted scope
- **THEN** the service denies the request without revealing document existence or content

### Requirement: Document versions are independently indexable

Each document version MUST have a stable identity, content fingerprint, processing status, and index state so rebuilds, incremental updates, and deletions are deterministic.

#### Scenario: Incremental document replacement

- **WHEN** a new version has a changed content fingerprint
- **THEN** only the new version is indexed and the previous version is removed from active retrieval

### Requirement: Retrieval returns provenance citations

Every retrieved context item MUST retain knowledge-base, document, version, chunk, title, and source-location metadata sufficient to produce a user-visible citation.

#### Scenario: Grounded answer includes citations

- **WHEN** the assistant answers using retrieved knowledge
- **THEN** the response includes citations for the supporting chunks and does not cite an unindexed or unauthorized source

### Requirement: RAG degrades safely on no hit

The service MUST distinguish no-match retrieval from retrieval failure and MUST avoid fabricating knowledge when no authorized relevant context is found.

#### Scenario: No relevant document matches

- **WHEN** authorized retrieval returns no relevant chunks
- **THEN** the assistant states that the knowledge base has no matching information and may continue with existing ticket tools or a clarification request

### Requirement: Retrieved content is untrusted context

The service MUST isolate retrieved document text from system instructions and tool policy, and MUST not execute instructions embedded in documents.

#### Scenario: Document contains prompt injection

- **WHEN** a retrieved chunk instructs the assistant to ignore policy or call an unrelated tool
- **THEN** the instruction is treated as quoted content only and cannot alter tool permissions, user scope, or system behavior

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
