package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.tool.AgentToolRegistry;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import com.wimone.enjoytix.agent.tool.AgentToolExecutionContext;
import com.wimone.enjoytix.agent.workflow.AgentWorkflowState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgentOrchestrator {
    private final AgentModelClient modelClient;
    private final AgentToolRegistry toolRegistry;
    private final AgentToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;
    private final AgentAuthoritativeResponsePolicy responsePolicy;
    private final int maxIterations;

    public AgentOrchestrator(AgentModelClient modelClient, AgentToolRegistry toolRegistry,
                             AgentToolExecutor toolExecutor, ObjectMapper objectMapper,
                             @Value("${agent.model.max-tool-rounds:3}") int maxIterations) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
        this.responsePolicy = new AgentAuthoritativeResponsePolicy(objectMapper);
        this.maxIterations = Math.max(1, maxIterations);
    }

    public AgentChatResult chat(String content) {
        return chat(content, null, null);
    }

    public AgentChatResult chat(String content, String conversationId, String runId) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        AgentWorkflowState workflow = AgentWorkflowState.initial(conversationId, runId, content.trim());
        List<AgentToolResult> toolResults = new ArrayList<>();
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            AgentModelResponse response = modelClient.complete(new AgentModelRequest(workflow.messages(), toolDefinitions()));
            if (response == null) {
                throw new AgentModelException("Agent model returned an empty response");
            }
            if (response.toolCalls().isEmpty()) {
                List<String> calledTools = workflow.toolNames();
                String answer = responsePolicy.answer(response.content(), calledTools, toolResults);
                String confirmationResponse = confirmationResponse(answer, calledTools, toolResults);
                return new AgentChatResult(answer, workflow.conversationId(), workflow.runId(), "COMPLETED",
                        calledTools, confirmationResponse, workflow.toolResults());
            }
            workflow = workflow.appendAssistant(response.content(), response.toolCalls());
            for (AgentToolCall call : response.toolCalls()) {
                if (call.name() == null || call.name().isBlank()) {
                    continue;
                }
                AgentToolResult result = toolExecutor.execute(call.name(), new AgentToolRequest(call.arguments()),
                        new AgentToolExecutionContext(workflow.conversationId(), workflow.runId()));
                toolResults.add(result);
                workflow = workflow.appendToolResult(call, result, serialize(result));
            }
        }
        throw new AgentModelException("Agent model exceeded maximum tool rounds");
    }

    private String confirmationResponse(String answer, List<String> calledTools, List<AgentToolResult> toolResults) {
        for (int index = 0; index < calledTools.size() && index < toolResults.size(); index++) {
            if ("create_order".equals(calledTools.get(index)) && !toolResults.get(index).success()) return answer;
        }
        return null;
    }

    private List<AgentModelToolDefinition> toolDefinitions() {
        return toolRegistry.list().stream()
                .map(tool -> new AgentModelToolDefinition(tool.name(), tool.description(), tool.inputSchema()))
                .toList();
    }

    private String serialize(AgentToolResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new AgentModelException("Unable to serialize agent tool result", ex);
        }
    }
}
