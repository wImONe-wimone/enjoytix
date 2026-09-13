package com.wimone.enjoytix.agent.tool;

public record AgentToolAuditEvent(String toolName, String subject, String conversationId, String runId,
                                  String outcome, String failureCategory, long durationMillis, int attempts) {
    public AgentToolAuditEvent(String toolName, Long userId, String status, String errorCode,
                               long durationMillis, int attempts) {
        this(toolName, userId == null ? "anonymous" : "user:" + userId, null, null,
                status, errorCode, durationMillis, attempts);
    }
}
