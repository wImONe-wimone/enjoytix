package com.wimone.enjoytix.agent.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunTest {
    @Test
    void transitionsFromCreatedToCompleted() {
        Instant created = Instant.parse("2026-09-12T00:00:00Z");
        Instant finished = Instant.parse("2026-09-12T00:00:01Z");
        AgentRun run = new AgentRun(1L, 2L, 3L, "question", AgentRun.Status.CREATED, created, null, null);

        AgentRun completed = run.start().complete(finished);

        assertThat(completed.status()).isEqualTo(AgentRun.Status.COMPLETED);
        assertThat(completed.createdAt()).isEqualTo(created);
        assertThat(completed.finishedAt()).isEqualTo(finished);
    }
}
