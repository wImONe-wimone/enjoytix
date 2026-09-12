package com.wimone.enjoytix.agent.remote.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PerformanceDetailResponse(
        Long performanceId,
        String title,
        String city,
        List<ShowSessionResponse> sessions) {
}