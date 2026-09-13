# AI Tool Security Specification

## Purpose

Protects ticketing tools and user data by enforcing trusted identity, mutation policies, auditing, and sanitized failures around AI-initiated actions.

## Requirements

### Requirement: Tools propagate authenticated identity

Every user-scoped tool MUST receive the authenticated user identity from the trusted request context rather than from model-generated arguments.

#### Scenario: Model attempts to query another user

- **WHEN** a tool request contains a user identifier that differs from the authenticated context
- **THEN** the tool ignores or rejects the model-supplied identifier
- **AND** no other user's data is returned

### Requirement: Side-effecting tools require policy checks

Order creation, cancellation, refund, and other mutating tools MUST enforce authentication, authorization, explicit confirmation, idempotency, and domain validation before invoking a remote service.

#### Scenario: Duplicate create-order request

- **WHEN** the same confirmed purchase request is submitted more than once with the same idempotency key
- **THEN** the tool returns the original outcome or a stable duplicate result
- **AND** it does not create a second order

### Requirement: Tool calls are auditable

The service MUST record tool name, conversation/run identifier, authenticated subject, outcome, latency, and failure category without recording secrets or unnecessary payment data.

#### Scenario: Tool invocation fails

- **WHEN** a remote tool call fails
- **THEN** an audit event records the failure category and correlation identifiers
- **AND** the assistant receives a sanitized failure result

### Requirement: Knowledge scope follows authenticated identity

Knowledge indexing and retrieval MUST derive tenant, subject, and permission scope from trusted application context rather than model-generated knowledge-base or user identifiers.

#### Scenario: Model attempts cross-scope retrieval

- **WHEN** a model-generated retrieval request names a knowledge base outside the authenticated scope
- **THEN** the request is rejected or constrained to the trusted scope
- **AND** no existence, metadata, or content from the other scope is disclosed

### Requirement: Retrieved instructions cannot expand tool authority

The service MUST treat document content as untrusted data and MUST preserve the existing tool allowlist, side-effect policy, and user identity rules regardless of retrieved text.

#### Scenario: Retrieved text requests an order mutation

- **WHEN** a document tells the assistant to create, cancel, or refund an order
- **THEN** the text cannot invoke the side-effecting tool
- **AND** normal confirmation, authorization, quote, and idempotency checks remain mandatory