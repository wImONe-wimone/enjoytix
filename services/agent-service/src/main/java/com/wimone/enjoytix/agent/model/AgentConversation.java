package com.wimone.enjoytix.agent.model;
import java.time.Instant;
import java.util.List;
public record AgentConversation(Long conversationId, Long userId, Instant createdAt, List<AgentMessage> messages) {}
