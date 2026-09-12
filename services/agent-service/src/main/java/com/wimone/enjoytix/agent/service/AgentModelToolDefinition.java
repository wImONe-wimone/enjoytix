package com.wimone.enjoytix.agent.service;

import java.util.Map;

public record AgentModelToolDefinition(String name, String description, Map<String, Object> inputSchema) {
    public AgentModelToolDefinition {
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    }
}