package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRuntimeDecision;
import com.wimone.enjoytix.agent.knowledge.rollout.RagRuntimePolicy;
import com.wimone.enjoytix.agent.knowledge.config.RagProperties;
import com.wimone.enjoytix.agent.knowledge.retrieval.AuthorizedKnowledgeRetrievalService;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult;
import com.wimone.enjoytix.agent.knowledge.rollout.RagFallbackReasons;
import org.springframework.beans.factory.ObjectProvider;
import com.wimone.enjoytix.agent.tool.AgentToolRegistry;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import com.wimone.enjoytix.agent.tool.AgentToolExecutionContext;
import com.wimone.enjoytix.agent.workflow.AgentWorkflowState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentOrchestrator {
    private final AgentModelClient modelClient;
    private final AgentToolRegistry toolRegistry;
    private final AgentToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;
    private final AgentAuthoritativeResponsePolicy responsePolicy;
    private final int maxIterations;
    private final RagRuntimePolicy ragRuntimePolicy;
    private final AuthorizedKnowledgeRetrievalService retrievalService;
    private final RagProperties ragProperties;

    public AgentOrchestrator(AgentModelClient modelClient, AgentToolRegistry toolRegistry,
                             AgentToolExecutor toolExecutor, ObjectMapper objectMapper,
                             @Value("${agent.model.max-tool-rounds:3}") int maxIterations) {
        this(modelClient, toolRegistry, toolExecutor, objectMapper, maxIterations, null, (AuthorizedKnowledgeRetrievalService) null, null);
    }

    public AgentOrchestrator(AgentModelClient modelClient, AgentToolRegistry toolRegistry,
                             AgentToolExecutor toolExecutor, ObjectMapper objectMapper,
                             int maxIterations, RagRuntimePolicy ragRuntimePolicy) {
        this(modelClient, toolRegistry, toolExecutor, objectMapper, maxIterations, ragRuntimePolicy, (AuthorizedKnowledgeRetrievalService) null, null);
    }

    @Autowired
    public AgentOrchestrator(AgentModelClient modelClient, AgentToolRegistry toolRegistry,
                             AgentToolExecutor toolExecutor, ObjectMapper objectMapper,
                             @Value("${agent.model.max-tool-rounds:3}") int maxIterations,
                             RagRuntimePolicy ragRuntimePolicy,
                             ObjectProvider<AuthorizedKnowledgeRetrievalService> retrievalService,
                             RagProperties ragProperties) {
        this(modelClient, toolRegistry, toolExecutor, objectMapper, maxIterations, ragRuntimePolicy,
                retrievalService == null ? null : retrievalService.getIfAvailable(), ragProperties);
    }

    private AgentOrchestrator(AgentModelClient modelClient, AgentToolRegistry toolRegistry,
                             AgentToolExecutor toolExecutor, ObjectMapper objectMapper,
                             int maxIterations, RagRuntimePolicy ragRuntimePolicy,
                             AuthorizedKnowledgeRetrievalService retrievalService,
                             RagProperties ragProperties) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
        this.responsePolicy = new AgentAuthoritativeResponsePolicy(objectMapper);
        this.maxIterations = Math.max(1, maxIterations);
        this.ragRuntimePolicy = ragRuntimePolicy;
        this.retrievalService = retrievalService;
        this.ragProperties = ragProperties;
    }
    public AgentChatResult chat(String content) {
        return chat(content, null, null);
    }

    public AgentChatResult chat(String content, String conversationId, String runId) {
        if (ragRuntimePolicy == null || retrievalService == null || ragProperties == null) return chatInternal(content, conversationId, runId, null);
        AgentUserContext user = AgentUserContextHolder.current();
        if (user == null) return chatInternal(content, conversationId, runId, null);
        String cohortKey = conversationId != null && !conversationId.isBlank() ? conversationId : runId;
        if (cohortKey == null || cohortKey.isBlank()) cohortKey = "agent";
        RagRuntimeDecision decision = ragRuntimePolicy.decide(user, cohortKey);
        if (!decision.ragEnabled()) return chatInternal(content, conversationId, runId, null);
        KnowledgeRetrievalResult retrievalResult;
        try {
            retrievalResult = retrievalService.retrieve(new KnowledgeRetrievalRequest(user, content, ragProperties.getTopK(), ragProperties.getMinimumScore()));
        } catch (RuntimeException failure) {
            retrievalResult = KnowledgeRetrievalResult.failure(failure, "RUNTIME");
        }
        List<RetrievedKnowledgeContext> contexts = retrievalResult.contexts().stream()
                .map(context -> new RetrievedKnowledgeContext(context.citationKey(), context.title(), context.sourceLocation(), context.content())).toList();
        Map<String, String> runtimeMetadata = new HashMap<>();
        runtimeMetadata.put("ragPath", "RAG_SELECTED");
        runtimeMetadata.put("cohortKey", cohortKey);
        if (retrievalResult.outcome() != com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalOutcome.SUCCESS) {
            runtimeMetadata.put("ragPath", "PRE_RAG");
            runtimeMetadata.put("fallbackReason", RagFallbackReasons.forRetrieval(retrievalResult));
        }
        return chatInternal(new KnowledgeContextPromptAssembler().assemble(content, contexts, retrievalResult),
                conversationId, runId, retrievalResult, runtimeMetadata);
    }
    public AgentChatResult chatWithKnowledgeContext(String content, String conversationId, String runId,
                                                    List<RetrievedKnowledgeContext> contexts) {
        return chatInternal(new KnowledgeContextPromptAssembler().assemble(content, contexts), conversationId, runId);
    }

    public AgentChatResult chatWithKnowledgeResult(String content, String conversationId, String runId,
                                                   List<RetrievedKnowledgeContext> contexts,
                                                   com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult retrievalResult) {
        return chatInternal(new KnowledgeContextPromptAssembler().assemble(content, contexts, retrievalResult),
                conversationId, runId, retrievalResult);
    }

    private AgentChatResult chatInternal(String content, String conversationId, String runId) {
        return chatInternal(content, conversationId, runId, null);
    }

    private AgentChatResult chatInternal(String content, String conversationId, String runId,
                                         com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult retrievalResult) {
        return chatInternal(content, conversationId, runId, retrievalResult, null);
    }

    private AgentChatResult chatInternal(String content, String conversationId, String runId,
                                         com.wimone.enjoytix.agent.knowledge.retrieval.KnowledgeRetrievalResult retrievalResult,
                                         Map<String, String> runtimeMetadataOverride) {
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
                Map<String, String> ragMetadata = new HashMap<>(runtimeMetadataOverride == null ? runtimeMetadata(conversationId, runId) : runtimeMetadataOverride);
                if (retrievalResult != null) {
                    retrievalResult.metadata().forEach(ragMetadata::putIfAbsent);
                    ragMetadata.putIfAbsent("outcome", retrievalResult.outcome().name());
                }
                return new AgentChatResult(answer, workflow.conversationId(), workflow.runId(), "COMPLETED",
                        calledTools, confirmationResponse, workflow.toolResults(),
                        retrievalResult == null ? List.of() : retrievalResult.citations(), ragMetadata);
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

    private Map<String, String> runtimeMetadata(String conversationId, String runId) {
        if (ragRuntimePolicy == null) return Map.of();
        AgentUserContext user = AgentUserContextHolder.current();
        if (user == null) return Map.of("ragPath", "PRE_RAG", "fallbackReason", RagRuntimePolicy.DISABLED);
        String cohortKey = conversationId != null && !conversationId.isBlank() ? conversationId : runId;
        if (cohortKey == null || cohortKey.isBlank()) cohortKey = "agent";
        RagRuntimeDecision decision = ragRuntimePolicy.decide(user, cohortKey);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ragPath", decision.ragEnabled() ? "RAG_SELECTED" : "PRE_RAG");
        metadata.put("cohortKey", decision.cohortKey());
        if (decision.fallbackReason() != null) metadata.put("fallbackReason", decision.fallbackReason());
        return Map.copyOf(metadata);
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
