package com.wimone.enjoytix.agent.knowledge.persistence;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRagSchemaMigrationTest {

    @Test
    void createsEmbeddingGenerationAndTombstoneSchemaOnCleanDatabase() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:knowledge_rag_schema;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V3__knowledge_rag_mysql.sql"));

        migration.execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> tables = jdbcTemplate.query(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE LOWER(TABLE_NAME) IN " +
                        "('agent_knowledge_index_generation', 'agent_knowledge_embedding', 'agent_knowledge_index_tombstone')",
                new SingleColumnRowMapper<>(String.class));

        assertThat(tables).containsExactlyInAnyOrder(
                "agent_knowledge_index_generation",
                "agent_knowledge_embedding",
                "agent_knowledge_index_tombstone");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE LOWER(TABLE_NAME) = 'agent_knowledge_embedding' " +
                        "AND LOWER(COLUMN_NAME) IN ('content_fingerprint', 'embedding_json', 'embedding_dimension', 'status')",
                Integer.class)).isEqualTo(4);
    }

    @Test
    void rollbackRemovesRagTablesFromCleanDatabase() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:knowledge_rag_rollback;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__knowledge_rag_mysql.sql"))
                .execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/rollback/U3__knowledge_rag_mysql.sql"))
                .execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE LOWER(TABLE_NAME) LIKE 'agent_knowledge_index_%'",
                Integer.class)).isZero();
    }
}
