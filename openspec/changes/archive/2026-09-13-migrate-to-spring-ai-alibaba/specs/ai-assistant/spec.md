# AI Assistant Specification

## ADDED Requirements

### Requirement: AI service provides conversational assistance

The AI service MUST accept an authenticated user message with an optional conversation identifier and return a structured assistant response containing the conversation identifier, answer text, and any tool execution summary.

#### Scenario: Answer a show availability question

- **WHEN** an authenticated user asks for available shows, sessions, ticket categories, or prices
- **THEN** the assistant invokes authoritative read-only tools and answers using their returned data
- **AND** the response does not invent availability, price, venue, or session information

### Requirement: AI uses controlled tool calling

The AI service MUST expose only an allowlisted set of ticketing tools to the model, with typed input and output contracts.

#### Scenario: Tool is selected for a ticketing question

- **WHEN** the model determines that a domain lookup is required
- **THEN** only the matching registered tool is executable
- **AND** tool errors are converted into safe assistant-visible results without leaking internal stack traces

### Requirement: AI supports confirmed purchase

The AI service MUST separate purchase quotation from order creation and MUST require explicit user confirmation before creating an order.

#### Scenario: User confirms a purchase

- **WHEN** the assistant has produced a current quote and the authenticated user explicitly confirms it
- **THEN** the purchase tool calls the existing order domain service with the current user context
- **AND** the response includes the authoritative order result

#### Scenario: User has not confirmed a purchase

- **WHEN** the user has not explicitly confirmed the quote or the quote has expired
- **THEN** the assistant MUST NOT create an order
- **AND** it returns a confirmation or quote-refresh request

### Requirement: AI degrades safely

The AI service MUST provide a deterministic error response when the configured model is disabled, unavailable, times out, or exceeds the tool-call round limit.

#### Scenario: Model is unavailable

- **WHEN** a model invocation fails or times out
- **THEN** the service returns a stable error code and user-safe message
- **AND** no order, payment, or inventory mutation is performed
