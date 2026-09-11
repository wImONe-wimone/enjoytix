package com.wimone.enjoytix.agent.service;

import java.util.stream.Stream;

public interface StreamingAgentResponseGenerator extends AgentResponseGenerator {
    Stream<String> generateStream(Long conversationId, Long userId, String content);
}
