package com.wimone.enjoytix.agent.service;

import java.util.stream.Stream;

public interface AgentResponseGenerator {
    String generate(Long conversationId, Long userId, String content);

    default Stream<String> generateStream(Long conversationId, Long userId, String content) {
        return Stream.of(generate(conversationId, userId, content));
    }
}
