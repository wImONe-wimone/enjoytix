package com.wimone.enjoytix.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentChatClientConfigurationTest {

    @Test
    void buildsAgentChatClientFromSpringAiBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient expected = mock(ChatClient.class);
        when(builder.build()).thenReturn(expected);

        ChatClient result = new AgentChatClientConfiguration().agentChatClient(builder);

        assertThat(result).isSameAs(expected);
    }
}
