package com.wimone.enjoytix.agent.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
@ConditionalOnExpression("'${agent.model.enabled:false}' != 'true' or '${agent.model.tool-calling-enabled:false}' != 'true'")
public class DefaultAgentResponseGenerator implements AgentResponseGenerator, StreamingAgentResponseGenerator {
    @Override
    public String generate(Long conversationId, Long userId, String content) {
        return "我已收到你的问题：\"" + content + "\"。当前智能导购服务正在接入演出数据，请稍后再试。";
    }

    @Override
    public Stream<String> generateStream(Long conversationId, Long userId, String content) {
        return Stream.of(generate(conversationId, userId, content));
    }
}
