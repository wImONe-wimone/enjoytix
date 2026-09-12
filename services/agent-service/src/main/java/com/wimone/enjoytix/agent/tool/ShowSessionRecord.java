package com.wimone.enjoytix.agent.tool;

import java.time.LocalDateTime;

public record ShowSessionRecord(Long showId, String city, LocalDateTime showTime, String title) {
}