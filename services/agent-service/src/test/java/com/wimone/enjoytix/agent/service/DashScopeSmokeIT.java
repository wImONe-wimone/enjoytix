package com.wimone.enjoytix.agent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dashscope-smoke")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+",
        disabledReason = "DashScope smoke test requires AI_DASHSCOPE_API_KEY")
class DashScopeSmokeIT {
    @Autowired
    private ChatModel chatModel;

    @Test
    void completesSimplePrompt() {
        ChatResponse response = chatModel.call(new Prompt("Reply with PONG."));

        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput().getText()).isNotBlank();
    }
}
