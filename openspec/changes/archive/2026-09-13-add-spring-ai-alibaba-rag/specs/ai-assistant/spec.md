# AI Assistant Specification

## MODIFIED Requirements

### Requirement: AI service provides conversational assistance

The AI service MUST accept an authenticated user message with an optional conversation identifier and return a structured assistant response containing the conversation identifier, answer text, any tool execution summary, and optional citations for authorized knowledge used in the answer.

#### Scenario: Answer a show availability question

- **WHEN** an authenticated user asks for available shows, sessions, ticket categories, or prices
- **THEN** the assistant invokes authoritative read-only tools and answers using their returned data
- **AND** the response does not invent availability, price, venue, or session information

#### Scenario: Answer from authorized knowledge

- **WHEN** an authenticated user asks a question covered by an authorized knowledge base
- **THEN** the assistant grounds the answer in retrieved context
- **AND** it returns source citations when context was used

#### Scenario: Knowledge retrieval has no match

- **WHEN** no authorized relevant knowledge is retrieved
- **THEN** the assistant does not present an unsupported knowledge answer as fact
- **AND** it uses the existing ticket tools, asks for clarification, or states that no matching knowledge was found
