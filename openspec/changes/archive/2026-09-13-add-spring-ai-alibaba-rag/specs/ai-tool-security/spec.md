# AI Tool Security Specification

## ADDED Requirements

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
