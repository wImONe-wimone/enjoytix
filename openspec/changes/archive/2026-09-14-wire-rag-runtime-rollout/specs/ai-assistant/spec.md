## MODIFIED Requirements

### Requirement: AI service provides conversational assistance

The AI service MUST accept an authenticated user message with an optional conversation identifier and return a structured assistant response containing the conversation identifier, answer text, any tool execution summary, and optional citations for authorized knowledge used in the answer. Before retrieval or context injection, the service MUST select either the RAG path or the pre-RAG path using the configured rollout policy.

#### Scenario: Answer from authorized knowledge

- **WHEN** an authenticated user asks a question covered by an authorized knowledge base and the rollout policy enables RAG for the request
- **THEN** the assistant grounds the answer in retrieved context
- **AND** it returns source citations when context was used

#### Scenario: Answer a show availability question

- **WHEN** an authenticated user asks for available shows, sessions, ticket categories, or prices
- **THEN** the assistant invokes authoritative read-only tools and answers using their returned data
- **AND** the response does not invent availability, price, venue, or session information

#### Scenario: Knowledge retrieval has no match

- **WHEN** no authorized relevant knowledge is retrieved
- **THEN** the assistant does not present an unsupported knowledge answer as fact
- **AND** it uses the existing ticket tools, asks for clarification, or states that no matching knowledge was found

#### Scenario: User is outside the RAG cohort

- **WHEN** the rollout policy does not select the authenticated user and request cohort
- **THEN** the assistant uses the existing pre-RAG path
- **AND** it does not inject retrieved context or emit RAG citations

#### Scenario: Rollback is active

- **WHEN** the explicit RAG rollback control is active
- **THEN** every assistant request uses the existing pre-RAG path regardless of internal-user or canary membership
- **AND** ticket tools, purchase confirmation, identity, and idempotency rules remain unchanged

#### Scenario: Retrieval fails after RAG selection

- **WHEN** the rollout policy selects RAG but retrieval or context assembly fails
- **THEN** the assistant records a classified fallback reason and continues through the safe pre-RAG path
- **AND** it does not fabricate citations or knowledge from the failed retrieval