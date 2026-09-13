# Ticket Inventory Specification

## Purpose

Ensures AI answers about ticket inventory, seating, pricing, and availability remain grounded in authoritative ticket-service data and current purchase state.

## Requirements

### Requirement: AI inventory answers are authoritative

AI responses about inventory, seat availability, ticket category, and price MUST be derived from the ticket service response for the current request.

#### Scenario: Inventory changes before purchase

- **WHEN** inventory or price changes after a quote is generated
- **THEN** the purchase flow refreshes or rejects the stale quote
- **AND** it does not create an order from stale model context
