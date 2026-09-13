package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentRun;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@Profile("mysql")
public class MySqlRunRepository implements RunRepository {

    private final JdbcTemplate jdbcTemplate;

    public MySqlRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AgentRun save(AgentRun run) {
        jdbcTemplate.update("""
                INSERT INTO agent_run (run_id, conversation_id, user_id, input, status, created_at, finished_at, error)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE conversation_id = VALUES(conversation_id), user_id = VALUES(user_id),
                    input = VALUES(input), status = VALUES(status), created_at = VALUES(created_at),
                    finished_at = VALUES(finished_at), error = VALUES(error)
                """, run.runId(), run.conversationId(), run.userId(), run.input(), run.status().name(),
                timestamp(run.createdAt()), timestamp(run.finishedAt()), run.error());
        return run;
    }

    @Override
    public AgentRun find(Long runId) {
        List<AgentRun> runs = jdbcTemplate.query("""
                SELECT run_id, conversation_id, user_id, input, status, created_at, finished_at, error
                FROM agent_run
                WHERE run_id = ?
                """, (resultSet, rowNumber) -> new AgentRun(
                resultSet.getLong("run_id"),
                resultSet.getLong("conversation_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("input"),
                AgentRun.Status.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                instant(resultSet.getTimestamp("finished_at")),
                resultSet.getString("error")), runId);
        return runs.isEmpty() ? null : runs.get(0);
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
