# Add Spring AI Alibaba RAG

## Why

The Agent assistant can currently call authoritative ticketing tools, but its knowledge module stops at document ingestion, parsing, chunking, and persistence. It cannot retrieve grounded knowledge or provide citations, so operational and venue information would require model memory or manual tool additions.

## What Changes

- Add a permission-aware knowledge retrieval capability for authenticated users and knowledge-base scopes.
- Define document version, chunk identity, citation, and source-provenance contracts for RAG responses.
- Integrate DashScope embeddings and a Spring AI `VectorStore` behind application-owned ports.
- Vectorize existing chunks with full rebuild, incremental upsert, and deletion synchronization.
- Add retrieval, optional reranking, citation-preserving context assembly, and safe injection into the existing `ChatClient` flow.
- Add prompt-injection isolation, no-hit fallback, retrieval-quality tests, and cross-tenant authorization tests.
- Add feature flags, gray-release controls, and metrics for recall, citation coverage, latency, token usage, and estimated cost.

## Capabilities

### New Capabilities

- `knowledge-retrieval`: permission-aware vector indexing, retrieval, provenance, citations, and RAG context assembly.

### Modified Capabilities

- `ai-assistant`: assistant responses may use grounded knowledge context and expose safe citations without changing existing ticketing contracts.
- `ai-tool-security`: knowledge retrieval must enforce authenticated scope and treat retrieved content as untrusted data.
- `ai-workflow-foundation`: RAG stages and metrics must participate in application-owned run correlation and feature-flagged execution.

## Impact

- Affected module: `services/agent-service`, especially `agent.knowledge`, model orchestration, configuration, and persistence.
- New dependencies: Spring AI embedding/vector-store abstractions and the selected DashScope embedding integration; exact versions must remain aligned with the existing Spring AI Alibaba platform.
- New infrastructure: a vector-store backend suitable for local tests and MySQL/object-storage deployments, plus DashScope credentials only for enabled environments.
- API impact: assistant responses gain optional citation metadata; existing responses remain valid when RAG is disabled or no documents match.
- Operational impact: indexing jobs, migration/rebuild controls, authorization policies, observability, and gray rollout are required before enabling RAG in production.
