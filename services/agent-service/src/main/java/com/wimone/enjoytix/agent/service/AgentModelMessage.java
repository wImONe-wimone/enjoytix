package com.wimone.enjoytix.agent.service;

import java.util.List;

public record AgentModelMessage(String role, String content, String name, String toolCallId,
                                List<AgentToolCall> toolCalls) {
    public AgentModelMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AgentModelMessage user(String content) {
        return new AgentModelMessage("user", content, null, null, List.of());
    }

    public static AgentModelMessage assistant(String content, List<AgentToolCall> toolCalls) {
        return new AgentModelMessage("assistant", content, null, null, toolCalls);
    }

    public static AgentModelMessage tool(String name, String callId, String content) {
        return new AgentModelMessage("tool", content, name, callId, List.of());
    }
}