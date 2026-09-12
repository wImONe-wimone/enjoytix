package com.wimone.enjoytix.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentTool;
import com.wimone.enjoytix.agent.tool.AgentToolRegistry;
import com.wimone.enjoytix.agent.tool.AgentToolRequest;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import com.wimone.enjoytix.agent.tool.InMemoryAgentToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listsRegisteredToolsThroughMcp() throws Exception {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool("search_show_sessions"));
        McpController controller = new McpController(registry, objectMapper);

        Map<?, ?> response = objectMapper.convertValue(controller.handle(new McpJsonRpcRequest(1, "tools/list", Map.of())), Map.class);

        assertThat(response.get("id")).isEqualTo(1);
        assertThat(((Map<?, ?>) response.get("result")).containsKey("tools")).isTrue();
        assertThat((List<?>) ((Map<?, ?>) response.get("result")).get("tools")).hasSize(1);
    }

    @Test
    void callsRegisteredToolThroughMcp() throws Exception {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool("search_show_sessions"));
        McpController controller = new McpController(registry, objectMapper);

        Map<?, ?> response = objectMapper.convertValue(controller.handle(new McpJsonRpcRequest(
                "call-1", "tools/call", Map.of("name", "search_show_sessions", "arguments", Map.of("city", "上海")))), Map.class);

        Map<?, ?> result = (Map<?, ?>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(false);
        assertThat((List<?>) result.get("content")).hasSize(1);
    }

    @Test
    void returnsJsonRpcErrorForUnknownMethod() {
        McpController controller = new McpController(new InMemoryAgentToolRegistry(), objectMapper);

        Map<?, ?> response = objectMapper.convertValue(controller.handle(new McpJsonRpcRequest(2, "unknown", Map.of())), Map.class);

        assertThat(response.containsKey("error")).isTrue();
        assertThat(((Map<?, ?>) response.get("error")).get("code")).isEqualTo(-32601);
    }

    private record StubTool(String name) implements AgentTool {
        @Override
        public String description() { return "stub tool"; }

        @Override
        public Map<String, Object> inputSchema() { return Map.of("type", "object"); }

        @Override
        public AgentToolResult execute(AgentToolRequest request) {
            return AgentToolResult.success(Map.of("matched", request.stringValue("city")));
        }
    }
}
