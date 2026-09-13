# User Ordering Specification

## ADDED Requirements

### Requirement: Order creation remains domain-owned

AI-assisted order creation MUST invoke the existing order service and MUST NOT bypass its inventory, pricing, user, payment, or idempotency rules.

#### Scenario: AI creates an order after confirmation

- **WHEN** the user confirms a non-expired quote
- **THEN** the existing order service remains the source of truth for order creation and validation
- **AND** a failed domain response prevents the AI layer from claiming success
