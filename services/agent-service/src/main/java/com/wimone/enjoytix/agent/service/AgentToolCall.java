package com.wimone.enjoytix.agent.service;

import java.util.Map;

public record AgentToolCall(String id, String name, Map<String, Object> arguments) {
    public AgentToolCall {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}