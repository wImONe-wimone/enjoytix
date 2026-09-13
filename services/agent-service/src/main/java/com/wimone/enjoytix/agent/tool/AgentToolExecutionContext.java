package com.wimone.enjoytix.agent.tool;

public record AgentToolExecutionContext(String conversationId, String runId) {
    public AgentToolExecutionContext {
        conversationId = normalize(conversationId);
        runId = normalize(runId);
    }
    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
