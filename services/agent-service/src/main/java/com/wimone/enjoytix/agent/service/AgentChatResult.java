package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeCitation;
import com.wimone.enjoytix.agent.workflow.AgentToolNodeResult;

import java.util.List;
import java.util.Map;

public record AgentChatResult(String answer, String conversationId, String runId, String status,
                              List<String> toolCalls, String confirmationResponse,
                              List<AgentToolNodeResult> toolResults,
                              List<KnowledgeCitation> citations,
                              Map<String, String> ragMetadata) {
    public AgentChatResult(String answer, String conversationId, String runId,
                           List<String> toolCalls, String confirmationResponse) {
        this(answer, conversationId, runId, "COMPLETED", toolCalls, confirmationResponse, List.of(), List.of(), Map.of());
    }

    public AgentChatResult(String answer, String conversationId, String runId, String status,
                           List<String> toolCalls, String confirmationResponse,
                           List<AgentToolNodeResult> toolResults) {
        this(answer, conversationId, runId, status, toolCalls, confirmationResponse, toolResults, List.of(), Map.of());
    }

    public AgentChatResult(String answer, List<String> toolCalls) {
        this(answer, null, null, "COMPLETED", toolCalls, null, List.of(), List.of(), Map.of());
    }

    public static AgentChatResult withRag(String answer, String conversationId, String runId,
                                          List<String> toolCalls, String confirmationResponse,
                                          List<KnowledgeCitation> citations,
                                          Map<String, String> ragMetadata) {
        return new AgentChatResult(answer, conversationId, runId, "COMPLETED", toolCalls,
                confirmationResponse, List.of(), citations, ragMetadata);
    }

    public AgentChatResult {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
        citations = citations == null ? List.of() : List.copyOf(citations);
        ragMetadata = ragMetadata == null ? Map.of() : Map.copyOf(ragMetadata);
    }
}