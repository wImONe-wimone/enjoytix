package com.wimone.enjoytix.agent.service;

import java.util.List;

public record AgentModelRequest(List<AgentModelMessage> messages, List<AgentModelToolDefinition> tools) {
    public AgentModelRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}