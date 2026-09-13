package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentMessage;
import com.wimone.enjoytix.agent.model.AgentRun;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlPersistenceRepositoryTest {

    private ConversationRepository conversationRepository;
    private RunRepository runRepository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:agent_mysql;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V2__agent_mysql.sql")).execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        conversationRepository = new MySqlConversationRepository(jdbcTemplate, transactionTemplate);
        runRepository = new MySqlRunRepository(jdbcTemplate);
    }

    @Test
    void persistsConversationMessagesAndRuns() {
        Instant createdAt = Instant.parse("2026-09-13T08:00:00Z");
        AgentConversation conversation = new AgentConversation(101L, 202L, createdAt, List.of(
                new AgentMessage("user", "??????", createdAt),
                new AgentMessage("assistant", "????", createdAt.plusSeconds(1))));
        AgentRun run = new AgentRun(303L, 101L, 202L, "??????", AgentRun.Status.COMPLETED,
                createdAt, createdAt.plusSeconds(2), null);

        conversationRepository.save(conversation);
        conversationRepository.append(101L, new AgentMessage("user", "?????", createdAt.plusSeconds(3)));
        runRepository.save(run);

        assertThat(conversationRepository.find(101L)).isEqualTo(new AgentConversation(101L, 202L, createdAt, List.of(
                new AgentMessage("user", "??????", createdAt),
                new AgentMessage("assistant", "????", createdAt.plusSeconds(1)),
                new AgentMessage("user", "?????", createdAt.plusSeconds(3)))));
        assertThat(runRepository.find(303L)).isEqualTo(run);
    }
}
