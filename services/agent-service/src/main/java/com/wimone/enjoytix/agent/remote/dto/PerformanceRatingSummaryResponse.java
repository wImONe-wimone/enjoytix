package com.wimone.enjoytix.agent.remote.dto;

import java.math.BigDecimal;

public record PerformanceRatingSummaryResponse(
        Long performanceId,
        Integer reviewCount,
        BigDecimal avgRating,
        Integer star1Count,
        Integer star2Count,
        Integer star3Count,
        Integer star4Count,
        Integer star5Count) {
}
