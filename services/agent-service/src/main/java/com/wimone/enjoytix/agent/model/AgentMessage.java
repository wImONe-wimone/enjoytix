package com.wimone.enjoytix.agent.model;
import java.time.Instant;
public record AgentMessage(String role, String content, Instant createdAt) {}
