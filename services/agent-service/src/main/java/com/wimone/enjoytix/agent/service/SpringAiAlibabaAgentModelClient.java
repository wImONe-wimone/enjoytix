package com.wimone.enjoytix.agent.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${agent.model.enabled:false}' == 'true' and '${agent.model.tool-calling-enabled:false}' == 'true' and '${agent.model.provider:disabled}' == 'dashscope'")
public class SpringAiAlibabaAgentModelClient implements AgentModelClient {
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public SpringAiAlibabaAgentModelClient(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentModelResponse complete(AgentModelRequest request) {
        try {
            List<Message> messages = request.messages().stream().map(this::toSpringMessage).toList();
            DashScopeChatOptions options = DashScopeChatOptions.builder()
                    .tools(request.tools().stream().map(this::toFunctionTool).toList())
                    .toolChoice("auto")
                    .internalToolExecutionEnabled(false)
                    .build();
            ChatResponse response = chatModel.call(new Prompt(messages, options));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new AgentModelException("Agent model returned an empty response");
            }
            AssistantMessage assistant = response.getResult().getOutput();
            List<AgentToolCall> toolCalls = assistant.getToolCalls().stream()
                    .map(call -> new AgentToolCall(call.id(), call.name(), readArguments(call.arguments())))
                    .toList();
            return new AgentModelResponse(assistant.getText(), toolCalls);
        } catch (AgentModelException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AgentModelException("Agent model request failed", ex);
        }
    }

    private Message toSpringMessage(AgentModelMessage message) {
        return switch (message.role()) {
            case "user" -> new UserMessage(message.content());
            case "assistant" -> AssistantMessage.builder()
                    .content(message.content() == null ? "" : message.content())
                    .properties(Map.of())
                    .toolCalls(message.toolCalls().stream()
                            .map(call -> new AssistantMessage.ToolCall(call.id(), "function", call.name(), writeArguments(call.arguments())))
                            .toList())
                    .media(List.of())
                    .build();
            case "tool" -> ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse(
                            message.toolCallId(), message.name(), message.content())))
                    .metadata(Map.of())
                    .build();
            default -> throw new AgentModelException("Unsupported agent message role: " + message.role());
        };
    }

    private DashScopeApiSpec.FunctionTool toFunctionTool(AgentModelToolDefinition definition) {
        return new DashScopeApiSpec.FunctionTool(
                DashScopeApiSpec.FunctionTool.Type.FUNCTION,
                new DashScopeApiSpec.FunctionTool.Function(definition.name(), definition.description(), definition.inputSchema()));
    }

    private Map<String, Object> readArguments(String arguments) {
        try {
            return arguments == null || arguments.isBlank() ? Map.of() : objectMapper.readValue(arguments, Map.class);
        } catch (JsonProcessingException ex) {
            throw new AgentModelException("Invalid agent tool arguments", ex);
        }
    }

    private String writeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException ex) {
            throw new AgentModelException("Unable to serialize agent tool arguments", ex);
        }
    }
}
