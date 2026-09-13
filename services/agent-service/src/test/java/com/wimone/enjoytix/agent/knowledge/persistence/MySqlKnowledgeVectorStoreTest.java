package com.wimone.enjoytix.agent.knowledge.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlKnowledgeVectorStoreTest {

    private JdbcTemplate jdbcTemplate;
    private MySqlKnowledgeVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:knowledge_vector_store;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__knowledge_rag_mysql.sql"))
                .execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("INSERT INTO agent_knowledge_index_generation " +
                "(generation_id, knowledge_base_id, document_id, version_id, content_fingerprint, status, total_chunks, embedded_chunks, active) " +
                "VALUES ('gen-1', 'kb-1', 'doc-1', 'v1', 'fp-1', 'READY', 2, 0, TRUE)");
        EmbeddingPort embeddingPort = texts -> texts.stream()
                .map(text -> text.contains("refund") ? new float[]{1.0f, 0.0f} : new float[]{0.0f, 1.0f})
                .toList();
        vectorStore = new MySqlKnowledgeVectorStore(jdbcTemplate, embeddingPort, new ObjectMapper());
    }

    @Test
    void addsSearchesByCosineScoreAndEnforcesKnowledgeBaseScope() {
        KnowledgeVectorStoreContract.assertAddSearchAndDelete(vectorStore);
    }
}
