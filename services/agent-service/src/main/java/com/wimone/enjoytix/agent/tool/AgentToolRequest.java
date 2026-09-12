package com.wimone.enjoytix.agent.tool;

import java.util.Map;

public record AgentToolRequest(Map<String, Object> arguments) {
    public AgentToolRequest {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    public String stringValue(String name) {
        Object value = arguments.get(name);
        return value == null ? null : value.toString().trim();
    }
}