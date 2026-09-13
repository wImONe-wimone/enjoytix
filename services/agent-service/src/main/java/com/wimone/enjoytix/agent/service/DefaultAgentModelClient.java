package com.wimone.enjoytix.agent.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${agent.model.enabled:false}' != 'true' or '${agent.model.tool-calling-enabled:false}' != 'true'")
public class DefaultAgentModelClient implements AgentModelClient {
    @Override
    public AgentModelResponse complete(AgentModelRequest request) {
        String content = request.messages().stream()
                .filter(message -> "user".equals(message.role()))
                .reduce((first, second) -> second)
                .map(AgentModelMessage::content)
                .orElse("");
        return AgentModelResponse.text("我已收到你的问题：\"" + content + "\"。当前未启用大模型服务。");
    }
}
