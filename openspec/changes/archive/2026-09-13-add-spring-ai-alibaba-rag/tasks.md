## 1. Contracts and configuration

- [x] 1.1 Define knowledge authorization, retrieval request/result, citation, and RAG outcome contracts; verify unit tests cover authorized scope, denied scope, no-hit, and failure outcomes.
- [x] 1.2 Add RAG/indexing/reranking/citation feature flags, top-k limits, score thresholds, and provider-neutral embedding settings; verify the service starts with all RAG flags disabled and no credentials.
- [x] 1.3 Add MySQL schema and migrations for embedding records, active document generations, content fingerprints, index status, and deletion tombstones; verify migration and rollback scripts execute on a clean database.

## 2. Spring AI embedding and vector store

- [x] 2.1 Add the Spring AI Alibaba DashScope embedding dependency aligned with the existing Spring AI versions; verify the dependency tree has one consistent Spring AI version and disabled mode remains credential-free.
- [x] 2.2 Implement the DashScope `EmbeddingModel` adapter behind an application-owned embedding port; verify deterministic test doubles cover batch embedding, provider error, timeout, and dimension validation.
- [x] 2.3 Implement a MySQL-backed Spring AI `VectorStore` adapter with metadata scope filters and cosine similarity search; verify top-k ordering, score thresholds, tenant isolation, and empty-index behavior.
- [x] 2.4 Add an in-memory vector-store test adapter and contract test suite shared by MySQL and test implementations; verify both adapters satisfy add, search, and delete semantics.

## 3. Versioned indexing lifecycle

- [x] 3.1 Map existing knowledge chunks and provenance to immutable index documents with stable version/fingerprint metadata; verify repeated indexing of an unchanged version is idempotent.
- [x] 3.2 Implement full rebuild and incremental upsert jobs with batch limits, retry state, and progress reporting; verify changed content creates a new generation without activating partial results.
- [x] 3.3 Implement deletion synchronization for removed documents, chunks, and superseded versions; verify stale vectors are excluded immediately and deletion retries are idempotent.
- [x] 3.4 Add indexing authorization checks and audit events without storing document secrets or raw sensitive content; verify unauthorized indexing is denied and audit payloads are sanitized.

## 4. Retrieval and assistant integration

- [x] 4.1 Implement authorized retrieval with active-version filtering, bounded top-k candidates, optional reranking, and classified retrieval failures; verify no result can escape the requested subject scope.
- [x] 4.2 Implement citation assembly that maps opaque citation keys to document title, version, chunk, and safe source location; verify citations are emitted only for retrieved authorized chunks.
- [x] 4.3 Add delimited untrusted-context prompt assembly to the existing ChatClient/tool flow; verify retrieved instructions cannot modify system policy, tool allowlists, user identity, or purchase confirmation rules.
- [x] 4.4 Add no-hit and retrieval-failure fallback behavior; verify the assistant avoids fabricated knowledge and can continue with ticket tools or request clarification.
- [x] 4.5 Extend assistant DTOs with backward-compatible optional citations and RAG metadata; verify existing non-RAG controller and purchase contract tests remain unchanged.

## 5. Security and quality validation

- [x] 5.1 Add cross-tenant retrieval tests for knowledge bases, document versions, chunks, and citations; verify denied requests reveal neither existence nor metadata.
- [x] 5.2 Add prompt-injection fixtures covering tool escalation, policy override, credential extraction, and user-scope changes; verify all are treated as quoted data.
- [x] 5.3 Add deterministic retrieval-quality evaluation fixtures for recall@k, ranking, citation coverage, and no-hit precision; verify configured quality thresholds are reported in test output.
- [x] 5.4 Add race and recovery tests for concurrent rebuild, version activation, deletion, embedding timeout, and partial batch retry; verify active retrieval never uses incomplete or deleted generations.

## 6. Observability and gray rollout

- [x] 6.1 Add metrics and structured audit fields for retrieval attempts, candidate/final counts, citation coverage, retrieval/model latency, tokens, estimated cost, and conversation/run IDs; verify secrets and raw document bodies are absent.
- [x] 6.2 Add health/readiness diagnostics for embedding and vector-store dependencies with disabled-mode tolerance; verify the service remains healthy when RAG is disabled or DashScope is unavailable.
- [x] 6.3 Add an internal/canary RAG profile and rollout controls with explicit rollback flags; verify cohort selection, disabled fallback, and restart-based rollback.
- [x] 6.4 Document indexing operations, permissions, rebuild/delete procedures, environment variables, quality thresholds, dashboards, and rollback steps; verify a new developer can run local deterministic RAG tests without provider credentials.
- [x] 6.5 Run the agent module suite, MVP flow, full reactor compile, RAG integration profile, and strict OpenSpec validation; verify the pre-RAG path and confirmed purchase flow remain green.
