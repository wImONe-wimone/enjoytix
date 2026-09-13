CREATE TABLE IF NOT EXISTS agent_conversation (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (conversation_id)
);

CREATE TABLE IF NOT EXISTS agent_message (
    conversation_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (conversation_id, sequence_no),
    CONSTRAINT fk_agent_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES agent_conversation(conversation_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS agent_run (
    run_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    input TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6),
    error TEXT,
    PRIMARY KEY (run_id),
    CONSTRAINT fk_agent_run_conversation
        FOREIGN KEY (conversation_id) REFERENCES agent_conversation(conversation_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_agent_run_conversation_id ON agent_run (conversation_id);
