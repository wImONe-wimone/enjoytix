# AI Tool Security Specification

## ADDED Requirements

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
