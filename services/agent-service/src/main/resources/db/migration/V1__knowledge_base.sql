CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS agent_knowledge_base (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    embedding_model VARCHAR(200) NOT NULL,
    embedding_dimension INTEGER NOT NULL CHECK (embedding_dimension > 0),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_knowledge_document (
    id VARCHAR(64) PRIMARY KEY,
    knowledge_base_id VARCHAR(64) NOT NULL REFERENCES agent_knowledge_base(id),
    name VARCHAR(300) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    effective_version_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_knowledge_document_version (
    id VARCHAR(128) PRIMARY KEY,
    document_id VARCHAR(64) NOT NULL REFERENCES agent_knowledge_document(id),
    version_no INTEGER NOT NULL CHECK (version_no > 0),
    checksum VARCHAR(128) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('pending', 'running', 'failed', 'success')),
    process_mode VARCHAR(32) NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 0 CHECK (chunk_count >= 0),
    failure_message TEXT,
    source_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    effective BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    CONSTRAINT uq_agent_document_version UNIQUE (document_id, version_no),
    CONSTRAINT uq_agent_document_checksum UNIQUE (document_id, checksum)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_agent_document_effective_version
    ON agent_knowledge_document_version(document_id) WHERE effective = TRUE;
CREATE INDEX IF NOT EXISTS idx_agent_document_version_status
    ON agent_knowledge_document_version(document_id, status);

ALTER TABLE agent_knowledge_document
    ADD CONSTRAINT fk_agent_document_effective_version
    FOREIGN KEY (effective_version_id) REFERENCES agent_knowledge_document_version(id);

CREATE TABLE IF NOT EXISTS agent_knowledge_chunk (
    id VARCHAR(128) PRIMARY KEY,
    version_id VARCHAR(128) NOT NULL REFERENCES agent_knowledge_document_version(id) ON DELETE CASCADE,
    ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
    content TEXT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    character_count INTEGER NOT NULL CHECK (character_count >= 0),
    token_count INTEGER NOT NULL CHECK (token_count >= 0),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    source_name VARCHAR(300) NOT NULL,
    source_page INTEGER,
    source_section VARCHAR(300),
    embedding vector,
    CONSTRAINT uq_agent_chunk_ordinal UNIQUE (version_id, ordinal),
    CONSTRAINT uq_agent_chunk_hash UNIQUE (version_id, content_hash)
);

CREATE INDEX IF NOT EXISTS idx_agent_chunk_version_ordinal ON agent_knowledge_chunk(version_id, ordinal);
CREATE INDEX IF NOT EXISTS idx_agent_chunk_metadata ON agent_knowledge_chunk USING GIN(metadata);

CREATE TABLE IF NOT EXISTS agent_knowledge_processing_log (
    id BIGSERIAL PRIMARY KEY,
    version_id VARCHAR(128) NOT NULL REFERENCES agent_knowledge_document_version(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_processing_log_version ON agent_knowledge_processing_log(version_id, created_at);

-- The vector dimension and HNSW/IVFFlat index are intentionally added after the first embedding model is selected.
