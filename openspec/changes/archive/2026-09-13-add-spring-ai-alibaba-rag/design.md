## Context

See `proposal.md` for the motivation. The Agent service already owns knowledge-base ingestion, document parsing, chunking, provenance metadata, MySQL repositories, and a Spring AI Alibaba `ChatClient` model path. Retrieval and indexing are not yet part of the runtime workflow.

## Goals / Non-Goals

**Goals:**

- Keep authorization, document lifecycle, citation metadata, and RAG policy in the application layer.
- Use Spring AI `EmbeddingModel` and `VectorStore` contracts while keeping the DashScope provider replaceable.
- Reuse existing chunk identities and MySQL deployment support for deterministic indexing and deletion.
- Inject retrieved text as clearly delimited, read-only context into the existing assistant flow.
- Make RAG independently feature-flagged, observable, testable, and reversible.

**Non-Goals:**

- Replacing the existing ticketing tools or order domain.
- Letting retrieved documents invoke tools, change user scope, or override system instructions.
- Introducing Spring AI Alibaba Graph in this change.
- Building a general-purpose document authoring or enterprise search product.

## Decisions

### Use application-owned retrieval ports around Spring AI

Define ports for authorization, embedding/index synchronization, retrieval, reranking, citation assembly, and RAG context policy. Adapt DashScope through Spring AI `EmbeddingModel` and adapt storage through the Spring AI `VectorStore` contract. This keeps provider and storage details out of assistant DTOs; direct provider SDK calls are rejected because they would recreate the coupling removed by the model migration.

### Use MySQL-backed vector persistence first

Store chunk embeddings, content fingerprints, active document version, scope metadata, and index status in MySQL, with an application adapter implementing `VectorStore` semantics and cosine similarity retrieval. Use an in-memory/simple vector store test adapter for fast unit tests. This avoids requiring a new infrastructure service for the first release; a future Milvus or pgvector adapter can replace the storage implementation without changing retrieval contracts.

### Treat document versions as immutable index inputs

Index keys include knowledge-base ID, document ID, version ID, and chunk ID. A new content fingerprint creates a new index generation; activation atomically switches retrieval to that generation, while stale generations are deleted asynchronously and retried safely. Deletion is a tombstone-plus-vector-delete operation so retries remain idempotent.

### Enforce scope before and after retrieval

Authorization resolves the trusted subject and allowed knowledge-base scopes before vector search. Scope filters are also applied to stored metadata and result assembly as defense in depth. Model-provided IDs are never used to broaden the search.

### Use bounded retrieval and optional reranking

Retrieve a small top-k candidate set using scope and active-version filters, then apply a configurable reranker only when enabled. If reranking or embedding services fail, return a classified retrieval failure and use the no-hit/knowledge-unavailable fallback rather than fabricating context.

### Delimit context and citations explicitly

The prompt builder places retrieved passages in a clearly marked untrusted-context section and instructs the model to use them as evidence, not commands. Each passage carries an opaque citation key mapped to document title, version, and source location in the response. Raw secrets, internal storage paths, and unauthorized metadata are excluded.

### Roll out with independent flags and metrics

Add separate flags for indexing, retrieval, citation emission, and gray cohort selection. Record conversation/run IDs, retrieval attempted/outcome, candidate and final counts, citation coverage, latency, token usage, and estimated cost. The disabled path remains the current non-RAG ChatClient/tool flow, and rollback is a configuration change followed by restart.

## Risks / Trade-offs

- [MySQL brute-force similarity may not scale to large corpora] → Start with bounded knowledge-base sizes and measure latency; keep the `VectorStore` adapter replaceable for a later ANN backend.
- [Embedding model or provider outage can block indexing/retrieval] → Make indexing asynchronous and retryable; classify runtime failures and preserve no-hit/normal assistant fallback.
- [Prompt injection in documents can influence responses] → Delimit context, require citation grounding, preserve tool policy outside retrieved text, and test malicious documents.
- [Version races can expose stale content] → Use immutable versions, active-generation filtering, fingerprints, and idempotent deletion/rebuild jobs.
- [Citations may reduce response compatibility] → Make citations optional additive DTO fields and retain existing answer/tool contracts when RAG is disabled.

## Migration Plan

1. Add schema/configuration and provider-neutral ports while keeping RAG disabled.
2. Implement DashScope embedding and MySQL `VectorStore` adapters with deterministic index/rebuild/delete jobs.
3. Add authorization-aware retrieval, citations, prompt-safe context assembly, and no-hit fallback.
4. Run unit, integration, security, indexing-race, and retrieval-quality tests with a local deterministic embedding substitute.
5. Enable indexing for a canary knowledge base, then enable retrieval for an internal cohort and compare latency, citation coverage, answer quality, and cost.
6. Expand the cohort only after operational thresholds pass. Roll back by disabling retrieval/citation flags; retain indexed data for diagnosis and re-enable after remediation.

## Open Questions

- The production threshold for corpus size and latency that triggers migration from MySQL similarity search to an ANN vector backend can be set from canary measurements without changing the contracts or rollout sequence.
