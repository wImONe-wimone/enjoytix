package com.wimone.enjoytix.agent.service;

import java.util.List;

public record AgentChatResult(String answer, List<String> toolCalls) {
    public AgentChatResult {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}