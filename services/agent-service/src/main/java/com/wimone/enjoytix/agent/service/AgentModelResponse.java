package com.wimone.enjoytix.agent.service;

import java.util.List;

public record AgentModelResponse(String content, List<AgentToolCall> toolCalls) {
    public AgentModelResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AgentModelResponse text(String content) {
        return new AgentModelResponse(content, List.of());
    }

    public static AgentModelResponse toolCalls(List<AgentToolCall> toolCalls) {
        return new AgentModelResponse(null, toolCalls);
    }
}