package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@Profile("mysql")
public class MySqlConversationRepository implements ConversationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public MySqlConversationRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public AgentConversation save(AgentConversation conversation) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("""
                    INSERT INTO agent_conversation (conversation_id, user_id, created_at)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), created_at = VALUES(created_at)
                    """, conversation.conversationId(), conversation.userId(), timestamp(conversation.createdAt()));
            jdbcTemplate.update("DELETE FROM agent_message WHERE conversation_id = ?", conversation.conversationId());
            for (int sequenceNumber = 0; sequenceNumber < conversation.messages().size(); sequenceNumber++) {
                AgentMessage message = conversation.messages().get(sequenceNumber);
                jdbcTemplate.update("""
                        INSERT INTO agent_message (conversation_id, sequence_no, role, content, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """, conversation.conversationId(), sequenceNumber, message.role(), message.content(),
                        timestamp(message.createdAt()));
            }
        });
        return conversation;
    }

    @Override
    public AgentConversation find(Long conversationId) {
        List<AgentConversation> conversations = jdbcTemplate.query("""
                SELECT conversation_id, user_id, created_at
                FROM agent_conversation
                WHERE conversation_id = ?
                """, (resultSet, rowNumber) -> new AgentConversation(
                resultSet.getLong("conversation_id"),
                resultSet.getLong("user_id"),
                resultSet.getTimestamp("created_at").toInstant(),
                List.of()), conversationId);
        if (conversations.isEmpty()) {
            return null;
        }
        AgentConversation conversation = conversations.get(0);
        List<AgentMessage> messages = jdbcTemplate.query("""
                SELECT role, content, created_at
                FROM agent_message
                WHERE conversation_id = ?
                ORDER BY sequence_no
                """, (resultSet, rowNumber) -> new AgentMessage(
                resultSet.getString("role"),
                resultSet.getString("content"),
                resultSet.getTimestamp("created_at").toInstant()), conversationId);
        return new AgentConversation(conversation.conversationId(), conversation.userId(), conversation.createdAt(), messages);
    }

    @Override
    public AgentConversation append(Long conversationId, AgentMessage message) {
        return transactionTemplate.execute(status -> {
            try {
                jdbcTemplate.queryForObject("""
                        SELECT conversation_id
                        FROM agent_conversation
                        WHERE conversation_id = ?
                        FOR UPDATE
                        """, Long.class, conversationId);
            } catch (DataAccessException exception) {
                return null;
            }
            Integer sequenceNumber = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(MAX(sequence_no), -1) + 1
                    FROM agent_message
                    WHERE conversation_id = ?
                    """, Integer.class, conversationId);
            jdbcTemplate.update("""
                    INSERT INTO agent_message (conversation_id, sequence_no, role, content, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, conversationId, sequenceNumber, message.role(), message.content(), timestamp(message.createdAt()));
            return find(conversationId);
        });
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
