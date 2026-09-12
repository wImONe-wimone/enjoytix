package com.wimone.enjoytix.agent.remote.dto;

import java.time.LocalDateTime;

public record ShowSessionResponse(Long showId, LocalDateTime showTime) {
}