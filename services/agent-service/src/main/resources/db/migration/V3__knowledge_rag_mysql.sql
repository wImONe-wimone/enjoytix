CREATE TABLE IF NOT EXISTS agent_knowledge_index_generation (
    generation_id VARCHAR(128) NOT NULL,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    version_id VARCHAR(128) NOT NULL,
    content_fingerprint VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    total_chunks INT NOT NULL DEFAULT 0,
    embedded_chunks INT NOT NULL DEFAULT 0,
    failure_message VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    activated_at DATETIME(6),
    completed_at DATETIME(6),
    PRIMARY KEY (generation_id),
    CONSTRAINT uq_knowledge_generation_fingerprint UNIQUE (version_id, content_fingerprint),
    CONSTRAINT ck_knowledge_generation_status CHECK (status IN ('PENDING', 'RUNNING', 'FAILED', 'READY', 'DELETED')),
    CONSTRAINT ck_knowledge_generation_counts CHECK (total_chunks >= 0 AND embedded_chunks >= 0 AND embedded_chunks <= total_chunks)
);

CREATE INDEX idx_knowledge_generation_scope
    ON agent_knowledge_index_generation (knowledge_base_id, document_id, version_id, active);
CREATE INDEX idx_knowledge_generation_status
    ON agent_knowledge_index_generation (status, created_at);

CREATE TABLE IF NOT EXISTS agent_knowledge_embedding (
    embedding_id VARCHAR(128) NOT NULL,
    generation_id VARCHAR(128) NOT NULL,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64) NOT NULL,
    version_id VARCHAR(128) NOT NULL,
    chunk_id VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    metadata_json JSON NOT NULL,
    content_fingerprint VARCHAR(128) NOT NULL,
    embedding_model VARCHAR(200) NOT NULL,
    embedding_dimension INT NOT NULL,
    embedding_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'READY',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (embedding_id),
    CONSTRAINT uq_knowledge_embedding_chunk UNIQUE (generation_id, chunk_id),
    CONSTRAINT ck_knowledge_embedding_dimension CHECK (embedding_dimension > 0),
    CONSTRAINT ck_knowledge_embedding_status CHECK (status IN ('READY', 'DELETED'))
);

CREATE INDEX idx_knowledge_embedding_scope
    ON agent_knowledge_embedding (knowledge_base_id, document_id, version_id, status);
CREATE INDEX idx_knowledge_embedding_generation
    ON agent_knowledge_embedding (generation_id, status);

CREATE TABLE IF NOT EXISTS agent_knowledge_index_tombstone (
    tombstone_id BIGINT NOT NULL AUTO_INCREMENT,
    knowledge_base_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(64),
    version_id VARCHAR(128),
    chunk_id VARCHAR(128),
    target_fingerprint VARCHAR(128),
    reason VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    PRIMARY KEY (tombstone_id),
    CONSTRAINT uq_knowledge_tombstone_target UNIQUE (
        knowledge_base_id, document_id, version_id, chunk_id, target_fingerprint
    ),
    CONSTRAINT ck_knowledge_tombstone_reason CHECK (reason IN ('DOCUMENT', 'VERSION', 'CHUNK', 'FINGERPRINT'))
);

CREATE INDEX idx_knowledge_tombstone_pending
    ON agent_knowledge_index_tombstone (processed_at, created_at);
