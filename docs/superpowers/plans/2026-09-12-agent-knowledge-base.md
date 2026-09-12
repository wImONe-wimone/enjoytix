# Agent Knowledge Base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 参考 `D:\Develop\ragent` 的成熟分层，为 EnjoyTix Agent 建立可版本化、可增量处理、可追溯来源的政策与服务知识库，并接入多路召回。

**Architecture:** 采用 `ragent` 已验证的四层结构：知识库域管理、文档 ingestion pipeline、解析/切块/Embedding 核心能力、RAG 检索与 Agent 工具。EnjoyTix 第一阶段仍将这些模块落在 `agent-service`，通过端口隔离基础设施；使用 RustFS S3 API 保存原始文件，PostgreSQL/PgVector 保存文档元数据、切块和向量，后续可替换为独立模块或服务。与 `ragent` 的 Milvus 可选实现不同，本项目的首个生产实现固定 PgVector，避免同时引入两套向量基础设施。

**Tech Stack:** Java 17、Spring Boot 3.0.x、现有 MyBatis-Plus/数据库框架、PostgreSQL + PgVector、RustFS S3 API、Apache Tika、单一 Embedding 提供商、现有 Micrometer、Sa-Token 和 Agent Tool Registry。

**Reference Implementation:** `D:\Develop\ragent`
- 知识库域：`bootstrap/src/main/java/com/nageoffer/ai/ragent/knowledge`
- ingestion：`bootstrap/src/main/java/com/nageoffer/ai/ragent/ingestion`
- 解析与切块：`bootstrap/src/main/java/com/nageoffer/ai/ragent/core/parser`、`core/chunk`
- 向量与检索：`bootstrap/src/main/java/com/nageoffer/ai/ragent/rag/core`
- Embedding 抽象：`infra-ai/src/main/java/com/nageoffer/ai/ragent/infra/embedding`
- 数据库基线：`resources/database/schema_pg.sql`
- 多路检索说明：`docs/multi-channel-retrieval.md`

**Spec:** `docs/superpowers/plans/2026-09-12-agent-platform-phased-implementation.md` Phase 2

## Global Constraints

- 不改变已完成的购票确认、订单创建和支付交接语义。
- 知识库回答必须携带有效文档版本和来源标识；不能引用已失效或未发布版本。
- 原始文档、Chunk 内容和检索日志不得记录用户敏感信息；日志必须脱敏。
- 处理流程采用 `pending -> running -> success/failed` 状态；只有完整成功的版本才可检索。
- 参考 `ragent` 的 `DocumentStatus`、`ProcessMode`、`SourceType`、处理日志和调度执行记录，但不复制其后台管理页面、RocketMQ、Milvus 或复杂 Pipeline DSL。
- 首个 MVP 支持上传文件和受控 URL 两种来源；RustFS 作为原始文档主存储，外部 S3 来源、定时抓取和更多远程来源作为后续扩展点。
- 每个行为变更遵守 TDD：先写失败测试，再实现最小代码，最后运行模块回归。

---

### Task 1: 固化知识库域模型与处理状态

**参考：** `ragent/knowledge/enums`、`knowledge/dao/entity`、`knowledge/service`

**Files:**
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/KnowledgeBase.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/KnowledgeDocument.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/KnowledgeDocumentVersion.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/KnowledgeChunk.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/KnowledgeSource.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/DocumentStatus.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/ProcessMode.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/domain/SourceType.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/domain/KnowledgeDomainContractTest.java`

- [x] Model knowledge base identity and embedding model/dimension configuration.
- [x] Model document source, MIME type, size, checksum, object key and current effective version.
- [x] Model version status, parser/chunker settings, processing timestamps and failure message.
- [x] Model chunk index, content hash, token/character counts, metadata and embedding status.
- [x] Test valid state transitions and ensure only a successful version can become effective.

### Task 2: Add persistence schema and adapter

**参考：** `ragent/resources/database/schema_pg.sql` 中的 `t_knowledge_base`、`t_knowledge_document`、`t_knowledge_chunk`、`t_knowledge_document_chunk_log`

**Files:**
- Modify: `services/agent-service/pom.xml`
- Create: `services/agent-service/src/main/resources/db/migration/V1__knowledge_base.sql`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/port/KnowledgeRepository.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/persistence/KnowledgeDocumentMapper.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/persistence/KnowledgeChunkMapper.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/persistence/MybatisKnowledgeRepository.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/persistence/KnowledgeRepositoryTest.java`

- [ ] Follow the repository's existing MyBatis-Plus and migration conventions instead of importing `ragent`'s full DAO stack.
- [ ] Create base/document/version/chunk/processing-log tables with checksum and version uniqueness constraints.
- [ ] Add a PgVector column using the selected Embedding dimension and an HNSW/IVFFlat index after dimension is confirmed.
- [ ] Store structured source metadata as JSONB while keeping retrieval-critical fields indexed.
- [ ] Implement atomic publication: old effective version changes only after the new version is complete.
- [ ] Test checksum idempotency, effective-version filtering, chunk replacement and failed-version exclusion.

### Task 3: Implement source fetching and RustFS storage

**参考：** `ragent/knowledge/handler/RemoteFileFetcher`、`ragent/ingestion/strategy/fetcher/S3Fetcher`、`LocalFileFetcher`、`HttpUrlFetcher`

**Files:**
- Modify: `services/agent-service/pom.xml`
- Modify: `services/agent-service/src/main/resources/application.yaml`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/source/DocumentFetcher.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/source/UploadDocumentFetcher.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/source/HttpDocumentFetcher.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/storage/KnowledgeObjectStorage.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/storage/S3KnowledgeObjectStorage.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/source/DocumentFetcherTest.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/storage/S3KnowledgeObjectStorageTest.java`

- [ ] Define bounded fetch size, connect/read timeout and allowed content types for upload and URL sources.
- [ ] Use deterministic RustFS object keys based on knowledge base, document identity, version and checksum.
- [ ] Preserve content type, original filename, byte size and checksum metadata.
- [ ] Keep endpoint, bucket, access key and secret in environment-backed configuration only.
- [ ] Test URL safety limits, key determinism, missing-object errors and secret redaction.

### Task 4: Build parser and chunking core

**参考：** `ragent/core/parser/TikaDocumentParser`、`ParseResult`、`ParsedDocument`、`Provenance`、`core/chunk/ChunkingStrategy`、`FixedSizeTextChunker`、`StructureAwareTextChunker`

**Files:**
- Modify: `services/agent-service/pom.xml`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/parse/KnowledgeParser.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/parse/TikaKnowledgeParser.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/parse/ParsedKnowledgeDocument.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/parse/KnowledgeProvenance.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/chunk/KnowledgeChunker.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/chunk/FixedSizeKnowledgeChunker.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/chunk/StructureAwareKnowledgeChunker.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/parse/TikaKnowledgeParserTest.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/chunk/KnowledgeChunkerTest.java`

- [ ] Parse text, Markdown, HTML, PDF and Office content through Tika.
- [ ] Preserve headings, page/section metadata and source provenance where available, following `ragent`'s structured block model.
- [ ] Support fixed-size and structure-aware strategies behind one `KnowledgeChunker` interface.
- [ ] Normalize whitespace, reject empty content and generate deterministic chunk IDs from version/hash/index.
- [ ] Test parsing failure, repeatability, overlap boundaries, heading preservation and table/list content.

### Task 5: Implement ingestion pipeline and incremental updates

**参考：** `ragent/ingestion/engine/IngestionEngine`、`IngestionTaskService`、`IngestionPipelineService`、`domain/settings`

**Files:**
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/ingest/KnowledgeIngestionRequest.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/ingest/KnowledgeIngestionService.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/ingest/KnowledgeIngestionPipeline.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/ingest/KnowledgeProcessingLog.java`
- Modify: `services/agent-service/src/main/resources/application.yaml`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/ingest/KnowledgeIngestionServiceTest.java`

- [ ] Execute fetch -> checksum -> parse -> chunk -> embed -> persist -> publish as explicit stages.
- [ ] Skip unchanged checksums and create a new version for changed content or parser/chunker/Embedding configuration.
- [ ] Record per-stage duration, chunk count, failure reason and retryable/non-retryable classification.
- [ ] Ensure partial processing never exposes chunks to retrieval and safely cleans or supersedes temporary data.
- [ ] Test no-op re-ingestion, changed source, successful publication, retry and failure recovery.

### Task 6: Add Embedding provider abstraction

**参考：** `ragent/infra-ai/embedding/EmbeddingClient`、`EmbeddingService`、`RoutingEmbeddingService`、`AbstractOpenAIStyleEmbeddingClient`

**Files:**
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/embedding/EmbeddingClient.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/embedding/EmbeddingService.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/embedding/OpenAiCompatibleEmbeddingClient.java`
- Modify: `services/agent-service/src/main/resources/application.yaml`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/embedding/EmbeddingServiceTest.java`

- [ ] Validate model name, vector dimension, batch size and input length before persistence.
- [ ] Use one OpenAI-compatible provider for the first implementation, keeping the interface ready for routing later.
- [ ] Add bounded timeout, retry policy and metric tags without logging document content.
- [ ] Test dimension mismatch, empty input, batching, transient failure and deterministic metadata storage.

### Task 7: Implement hybrid retrieval and citations

**参考：** `ragent/rag/core/retrieve/RetrieverService`、`RetrievalEngine`、`MilvusRetrieverService`、`docs/multi-channel-retrieval.md`

**Files:**
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/retrieve/KnowledgeRetriever.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/retrieve/KnowledgeRetrievalRequest.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/retrieve/KnowledgeRetrievalResult.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/retrieve/KnowledgeCitation.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/retrieve/PgVectorKnowledgeRetriever.java`
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/knowledge/retrieve/HybridKnowledgeRetriever.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/retrieve/HybridKnowledgeRetrieverTest.java`

- [ ] Define a request with query, top-k, knowledge base, metadata filters and effective-time semantics.
- [ ] Implement vector similarity and PostgreSQL keyword search as independent retrieval routes.
- [ ] Add FAQ exact/normalized match and metadata filtering before merge/ranking.
- [ ] De-duplicate candidates, apply configurable route weights and return deterministic ordering.
- [ ] Return document title, version, section/page, checksum/reference and score for every citation.
- [ ] Test route fallback, no-result behavior, effective-version filtering, deduplication and ranking determinism.

### Task 8: Expose read-only knowledge search to Agent

**参考：** EnjoyTix 现有 `AgentToolRegistry`、`RegistryAgentToolExecutor`、审计 Sink；参考 `ragent/rag` 的查询入口但不复制其前端或复杂对话编排。

**Files:**
- Create: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/tool/KnowledgeSearchTool.java`
- Modify: `services/agent-service/src/main/java/com/wimone/enjoytix/agent/tool/AgentToolConfiguration.java`
- Modify: `services/agent-service/src/main/resources/application.yaml`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/tool/KnowledgeSearchToolTest.java`
- Test: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/KnowledgeSecurityTest.java`

- [ ] Register a bounded, read-only `knowledge_search` tool.
- [ ] Pass user context and allowed metadata filters into retrieval; never accept arbitrary database predicates.
- [ ] Make citations mandatory and distinguish “no source found” from a provider failure.
- [ ] Add retrieval latency, route hit rate, failure rate and citation coverage metrics.
- [ ] Test prompt-injection text in documents, metadata overreach, sensitive-log redaction and tool timeout.

### Task 9: Add local infrastructure, operations documentation and acceptance tests

**Files:**
- Create or modify: `deploy/` PostgreSQL/PgVector and RustFS local configuration following repository conventions
- Modify: `docs/architecture/agent-contracts.md`
- Create: `docs/architecture/agent-knowledge-base.md`
- Create: `services/agent-service/src/test/java/com/wimone/enjoytix/agent/knowledge/KnowledgeAcceptanceTest.java`

- [ ] Add local startup configuration without committing credentials; document required environment variables.
- [ ] Document ingestion states, migration order, supported file types, chunk strategies and re-index procedure.
- [ ] Document effective-version selection, citation format, retention and failed-task recovery.
- [ ] Add an end-to-end policy-question fixture whose answer must cite the current document version.
- [ ] Run `mvn -pl services/agent-service -am test`, `git diff --check` and review the final diff for unrelated order/payment changes.
