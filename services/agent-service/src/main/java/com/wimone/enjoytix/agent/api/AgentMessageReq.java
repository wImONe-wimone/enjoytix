package com.wimone.enjoytix.agent.api;

import jakarta.validation.constraints.NotBlank;

public record AgentMessageReq(@NotBlank String content) {
}
