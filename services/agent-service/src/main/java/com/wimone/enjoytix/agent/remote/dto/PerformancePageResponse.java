package com.wimone.enjoytix.agent.remote.dto;

public final class PerformancePageResponse {
    private PerformancePageResponse() {
    }

    public record PerformanceSummary(Long performanceId, String title, String city) {
    }
}