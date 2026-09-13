package com.wimone.enjoytix.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.knowledge.embedding.EmbeddingPort;
import com.wimone.enjoytix.agent.knowledge.persistence.MySqlKnowledgeVectorStore;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@Profile("mysql")
@EnableConfigurationProperties(AgentMySqlProperties.class)
public class MySqlPersistenceConfiguration {

    @Bean(destroyMethod = "close")
    public HikariDataSource agentDataSource(AgentMySqlProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        return dataSource;
    }

    @Bean
    public JdbcTemplate agentJdbcTemplate(DataSource agentDataSource) {
        return new JdbcTemplate(agentDataSource);
    }

    @Bean
    public PlatformTransactionManager agentTransactionManager(DataSource agentDataSource) {
        return new DataSourceTransactionManager(agentDataSource);
    }

    @Bean
    public TransactionTemplate agentTransactionTemplate(PlatformTransactionManager agentTransactionManager) {
        return new TransactionTemplate(agentTransactionManager);
    }

    @Bean
    @ConditionalOnBean(EmbeddingPort.class)
    public MySqlKnowledgeVectorStore knowledgeVectorStore(JdbcTemplate agentJdbcTemplate,
                                                          EmbeddingPort embeddingPort,
                                                          ObjectMapper objectMapper) {
        return new MySqlKnowledgeVectorStore(agentJdbcTemplate, embeddingPort, objectMapper);
    }

    @Bean
    public InitializingBean agentMySqlSchemaInitializer(DataSource agentDataSource) {
        return () -> new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V2__agent_mysql.sql"),
                new ClassPathResource("db/migration/V3__knowledge_rag_mysql.sql"))
                .execute(agentDataSource);
    }
}
