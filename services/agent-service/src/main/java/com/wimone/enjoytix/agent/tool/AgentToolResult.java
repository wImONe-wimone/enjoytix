package com.wimone.enjoytix.agent.tool;

public record AgentToolResult(boolean success, Object data, String errorCode, String errorMessage) {
    public static AgentToolResult success(Object data) {
        return new AgentToolResult(true, data, null, null);
    }

    public static AgentToolResult failure(String errorCode, String errorMessage) {
        return new AgentToolResult(false, null, errorCode, errorMessage);
    }
}