package com.wimone.enjoytix.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ChatModel.class)
public class AgentChatClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    public ChatClient agentChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
