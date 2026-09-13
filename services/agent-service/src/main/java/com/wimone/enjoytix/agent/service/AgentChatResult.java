package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.workflow.AgentToolNodeResult;

import java.util.List;

public record AgentChatResult(String answer, String conversationId, String runId, String status,
                              List<String> toolCalls, String confirmationResponse,
                              List<AgentToolNodeResult> toolResults) {
    public AgentChatResult(String answer, String conversationId, String runId,
                           List<String> toolCalls, String confirmationResponse) {
        this(answer, conversationId, runId, "COMPLETED", toolCalls, confirmationResponse, List.of());
    }

    public AgentChatResult(String answer, List<String> toolCalls) {
        this(answer, null, null, "COMPLETED", toolCalls, null, List.of());
    }

    public AgentChatResult {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
    }
}
