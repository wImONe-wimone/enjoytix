package com.wimone.enjoytix.agent.model;

import java.time.Instant;

public record AgentRun(Long runId, Long conversationId, Long userId, String input,
                       Status status, Instant createdAt, Instant finishedAt, String error) {

    public enum Status {
        CREATED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public AgentRun start() {
        return new AgentRun(runId, conversationId, userId, input, Status.RUNNING, createdAt, null, null);
    }

    public AgentRun complete(Instant finishedAt) {
        return new AgentRun(runId, conversationId, userId, input, Status.COMPLETED, createdAt, finishedAt, null);
    }

    public AgentRun fail(Instant finishedAt, String error) {
        return new AgentRun(runId, conversationId, userId, input, Status.FAILED, createdAt, finishedAt, error);
    }

    public AgentRun cancel(Instant finishedAt) {
        return new AgentRun(runId, conversationId, userId, input, Status.CANCELLED, createdAt, finishedAt, null);
    }
}
