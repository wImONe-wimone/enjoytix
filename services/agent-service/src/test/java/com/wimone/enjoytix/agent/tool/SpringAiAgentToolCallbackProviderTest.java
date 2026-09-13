package com.wimone.enjoytix.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiAgentToolCallbackProviderTest {
    @org.junit.jupiter.api.AfterEach
    void clearContext() {
        AgentUserContextHolder.clear();
    }
    @Test
    void exposesOnlyToolsRegisteredInApplicationAllowlist() {
        AgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool("allowed_tool"));
        AgentToolExecutor executor = mock(AgentToolExecutor.class);

        ToolCallback[] callbacks = new SpringAiAgentToolCallbackProvider(registry, executor, new ObjectMapper())
                .getToolCallbacks();

        assertThat(callbacks).singleElement().satisfies(callback ->
                assertThat(callback.getToolDefinition().name()).isEqualTo("allowed_tool"));
    }

    @Test
    void callbackDelegatesJsonArgumentsToTrustedExecutor() throws Exception {
        AgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool("allowed_tool"));
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        when(executor.execute(eq("allowed_tool"), any(AgentToolRequest.class)))
                .thenReturn(AgentToolResult.success(Map.of("ok", true)));
        ToolCallback callback = new SpringAiAgentToolCallbackProvider(registry, executor, new ObjectMapper())
                .getToolCallbacks()[0];

        String result = callback.call(new ObjectMapper().writeValueAsString(Map.of("showId", 12)));

        verify(executor).execute(eq("allowed_tool"), eq(new AgentToolRequest(Map.of("showId", 12))));
        assertThat(result).contains("success").contains("true").contains("ok");
    }

    @Test
    void callbackReturnsSanitizedFailureFromExecutor() {
        AgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool("allowed_tool"));
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        when(executor.execute(eq("allowed_tool"), any(AgentToolRequest.class)))
                .thenReturn(AgentToolResult.failure("FORBIDDEN", "Unable to access resource"));
        ToolCallback callback = new SpringAiAgentToolCallbackProvider(registry, executor, new ObjectMapper())
                .getToolCallbacks()[0];

        String result = callback.call("{}");

        assertThat(result).contains("success").contains("false").contains("FORBIDDEN")
                .doesNotContain("stackTrace");
    }

    @Test
    void callbackRejectsModelSuppliedUserIdForUserScopedTool() throws Exception {
        AgentToolRegistry registry = new InMemoryAgentToolRegistry();
        registry.register(new StubTool("get_current_user"));
        AgentToolExecutor executor = mock(AgentToolExecutor.class);
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        ToolCallback callback = new SpringAiAgentToolCallbackProvider(registry, executor, new ObjectMapper())
                .getToolCallbacks()[0];

        String result = callback.call(new ObjectMapper().writeValueAsString(Map.of("userId", 999L)));

        assertThat(result).contains("INVALID_ARGUMENT").contains("authenticated context");
        org.mockito.Mockito.verifyNoInteractions(executor);
    }

    private record StubTool(String name) implements AgentTool {
        @Override
        public String description() {
            return "stub";
        }

        @Override
        public Map<String, Object> inputSchema() {
            return Map.of("type", "object");
        }

        @Override
        public AgentToolResult execute(AgentToolRequest request) {
            return AgentToolResult.success(Map.of());
        }
    }
}
