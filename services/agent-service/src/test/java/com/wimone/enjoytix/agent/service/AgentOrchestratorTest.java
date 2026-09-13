package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentTool;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.tool.AgentToolExecutionContext;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import com.wimone.enjoytix.agent.workflow.AgentToolNodeResult;
import com.wimone.enjoytix.agent.tool.InMemoryAgentToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AgentOrchestratorTest {

    @Test
    void identifiesIntentCallsShowSessionToolAndSummarizesAnswer() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        AtomicInteger toolCalls = new AtomicInteger();
        registry.register(new AgentTool() {
            @Override
            public String name() {
                return "search_show_sessions";
            }

            @Override
            public String description() {
                return "Search show sessions";
            }

            @Override
            public Map<String, Object> inputSchema() {
                return Map.of("type", "object");
            }

            @Override
            public AgentToolResult execute(AgentToolRequest request) {
                toolCalls.incrementAndGet();
                assertThat(request.stringValue("city")).isEqualTo("上海");
                return AgentToolResult.success(List.of(Map.of("title", "音乐剧", "showTime", "2026-09-20T19:30:00")));
            }
        });

        AgentModelClient model = new AgentModelClient() {
            private int round;

            @Override
            public AgentModelResponse complete(AgentModelRequest request) {
                if (round++ == 0) {
                    return AgentModelResponse.toolCalls(List.of(
                            new AgentToolCall("call-1", "search_show_sessions", Map.of("city", "上海"))));
                }
                assertThat(request.messages()).anyMatch(message -> "tool".equals(message.role()));
                return AgentModelResponse.text("上海有一场音乐剧，时间是 2026 年 9 月 20 日 19:30。");
            }
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(model, registry,
                (AgentToolExecutor) (name, request) -> registry.find(name)
                        .map(tool -> tool.execute(request))
                        .orElseGet(() -> AgentToolResult.failure("TOOL_NOT_FOUND", name)),
                new ObjectMapper(), 3);

        AgentChatResult result = orchestrator.chat("帮我查上海的演出");

        assertThat(result.answer()).contains("音乐剧");
        assertThat(result.toolCalls()).containsExactly("search_show_sessions");
        assertThat(toolCalls).hasValue(1);
    }

    @Test
    void exposesNodeOrientedToolResultsWithWorkflowCorrelation() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        AgentModelClient model = new AgentModelClient() {
            private int round;

            @Override
            public AgentModelResponse complete(AgentModelRequest request) {
                if (round++ == 0) {
                    return AgentModelResponse.toolCalls(List.of(
                            new AgentToolCall("call-show", "search_show_sessions", Map.of("city", "Shanghai")),
                            new AgentToolCall("call-order", "lookup_order", Map.of("orderId", "order-1"))));
                }
                return AgentModelResponse.text("Authoritative answer");
            }
        };
        List<AgentToolExecutionContext> contexts = new java.util.ArrayList<>();
        AgentToolExecutor executor = new AgentToolExecutor() {
            @Override
            public AgentToolResult execute(String toolName, AgentToolRequest request) {
                throw new UnsupportedOperationException("Workflow context is required");
            }

            @Override
            public AgentToolResult execute(String toolName, AgentToolRequest request, AgentToolExecutionContext context) {
                contexts.add(context);
                if ("lookup_order".equals(toolName)) {
                    return AgentToolResult.failure("ORDER_NOT_FOUND", "Order is not available");
                }
                return AgentToolResult.success(List.of());
            }
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(model, registry, executor, new ObjectMapper(), 3);

        AgentChatResult result = orchestrator.chat("Find my show and order", "conversation-1", "run-1");

        assertThat(result.conversationId()).isEqualTo("conversation-1");
        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.toolCalls()).containsExactly("search_show_sessions", "lookup_order");
        assertThat(result.toolResults()).containsExactly(
                new AgentToolNodeResult("call-show", "search_show_sessions", true, null, null),
                new AgentToolNodeResult("call-order", "lookup_order", false, "ORDER_NOT_FOUND", "Order is not available"));
        assertThat(contexts).containsOnly(new AgentToolExecutionContext("conversation-1", "run-1"));
    }
}
