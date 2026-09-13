package com.wimone.enjoytix.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.Map;

@Component
public class SpringAiAgentToolCallbackProvider implements ToolCallbackProvider {
    private static final Set<String> USER_SCOPED_TOOLS = Set.of(
            "get_current_user", "list_current_user_orders", "get_current_user_order");
    private final AgentToolRegistry registry;
    private final AgentToolExecutor executor;
    private final ObjectMapper objectMapper;

    public SpringAiAgentToolCallbackProvider(AgentToolRegistry registry, AgentToolExecutor executor,
                                             ObjectMapper objectMapper) {
        this.registry = registry;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return registry.list().stream().map(this::toCallback).toArray(ToolCallback[]::new);
    }

    private ToolCallback toCallback(AgentTool tool) {
        return FunctionToolCallback.<Map<String, Object>, AgentToolResult>builder(tool.name(),
                        arguments -> execute(tool, arguments))
                .description(tool.description())
                .inputType(Map.class)
                .inputSchema(writeSchema(tool.inputSchema()))
                .toolCallResultConverter(this::writeResult)
                .build();
    }

    private AgentToolResult execute(AgentTool tool, Map<String, Object> arguments) {
        if (USER_SCOPED_TOOLS.contains(tool.name()) && arguments != null && arguments.containsKey("userId")) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "userId is controlled by authenticated context");
        }
        return executor.execute(tool.name(), new AgentToolRequest(arguments));
    }

    private String writeSchema(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema == null ? Map.of() : schema);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize agent tool schema", ex);
        }
    }

    private String writeResult(Object result, Type targetType) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            return writeSafeFailure();
        }
    }

    private String writeSafeFailure() {
        try {
            return objectMapper.writeValueAsString(AgentToolResult.failure(
                    "TOOL_RESULT_SERIALIZATION_FAILED", "Unable to serialize agent tool result"));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
