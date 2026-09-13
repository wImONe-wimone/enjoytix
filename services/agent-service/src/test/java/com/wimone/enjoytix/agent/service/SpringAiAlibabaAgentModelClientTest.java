package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiAlibabaAgentModelClientTest {
    private final ChatModel chatModel = mock(ChatModel.class);
    private final ChatClient chatClient = ChatClient.create(chatModel);
    private final SpringAiAlibabaAgentModelClient client =
            new SpringAiAlibabaAgentModelClient(chatClient, new ObjectMapper());

    @Test
    void mapsTextResponse() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response(new AssistantMessage("hello")));

        AgentModelResponse result = client.complete(new AgentModelRequest(
                List.of(AgentModelMessage.user("hi")), List.of()));

        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.toolCalls()).isEmpty();
    }

    @Test
    void mapsToolCallResponse() {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("")
                .properties(Map.of())
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "search_show_sessions",
                        "{\"city\":\"Shanghai\"}")))
                .build();
        when(chatModel.call(any(Prompt.class))).thenReturn(response(assistant));

        AgentModelResponse result = client.complete(new AgentModelRequest(
                List.of(AgentModelMessage.user("find shows")),
                List.of(new AgentModelToolDefinition("search_show_sessions", "Search", Map.of("type", "object")))));

        assertThat(result.toolCalls()).singleElement().satisfies(call -> {
            assertThat(call.id()).isEqualTo("call-1");
            assertThat(call.name()).isEqualTo("search_show_sessions");
            assertThat(call.arguments()).containsEntry("city", "Shanghai");
        });
    }

    @Test
    void rejectsInvalidToolArguments() {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("")
                .properties(Map.of())
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "search", "not-json")))
                .build();
        when(chatModel.call(any(Prompt.class))).thenReturn(response(assistant));

        assertThatThrownBy(() -> client.complete(new AgentModelRequest(
                List.of(AgentModelMessage.user("find")), List.of())))
                .isInstanceOf(AgentModelException.class)
                .hasMessage("Invalid agent tool arguments");
    }

    @Test
    void wrapsProviderFailure() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider down"));

        assertThatThrownBy(() -> client.complete(new AgentModelRequest(
                List.of(AgentModelMessage.user("hi")), List.of())))
                .isInstanceOf(AgentModelException.class)
                .hasMessage("Agent model request failed")
                .hasRootCauseMessage("provider down");
    }

    @Test
    void wrapsProviderTimeout() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException(new TimeoutException()));

        assertThatThrownBy(() -> client.complete(new AgentModelRequest(List.of(), List.of())))
                .isInstanceOf(AgentModelException.class)
                .hasRootCauseInstanceOf(TimeoutException.class);
    }

    private ChatResponse response(AssistantMessage assistant) {
        return new ChatResponse(List.of(new Generation(assistant)));
    }
}
