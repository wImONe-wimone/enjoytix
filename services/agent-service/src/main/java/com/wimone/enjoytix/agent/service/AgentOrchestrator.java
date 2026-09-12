package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.tool.AgentToolRegistry;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
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
    private final int maxIterations;

    public AgentOrchestrator(AgentModelClient modelClient, AgentToolRegistry toolRegistry,
                             AgentToolExecutor toolExecutor, ObjectMapper objectMapper,
                             @Value("${agent.model.max-tool-rounds:3}") int maxIterations) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
        this.maxIterations = Math.max(1, maxIterations);
    }

    public AgentChatResult chat(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        List<AgentModelMessage> messages = new ArrayList<>(List.of(AgentModelMessage.user(content.trim())));
        List<String> calledTools = new ArrayList<>();
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            AgentModelResponse response = modelClient.complete(new AgentModelRequest(messages, toolDefinitions()));
            if (response == null) {
                throw new AgentModelException("Agent model returned an empty response");
            }
            if (response.toolCalls().isEmpty()) {
                return new AgentChatResult(response.content() == null ? "" : response.content(), calledTools);
            }
            messages.add(AgentModelMessage.assistant(response.content(), response.toolCalls()));
            for (AgentToolCall call : response.toolCalls()) {
                if (call.name() == null || call.name().isBlank()) {
                    continue;
                }
                calledTools.add(call.name());
                AgentToolResult result = toolExecutor.execute(call.name(), new AgentToolRequest(call.arguments()));
                messages.add(AgentModelMessage.tool(call.name(), call.id(), serialize(result)));
            }
        }
        throw new AgentModelException("Agent model exceeded maximum tool rounds");
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