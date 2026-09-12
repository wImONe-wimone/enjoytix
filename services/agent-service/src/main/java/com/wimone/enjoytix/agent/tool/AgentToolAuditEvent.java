package com.wimone.enjoytix.agent.tool;

public record AgentToolAuditEvent(String toolName, Long userId, String status, String errorCode, long durationMillis, int attempts) {
}
