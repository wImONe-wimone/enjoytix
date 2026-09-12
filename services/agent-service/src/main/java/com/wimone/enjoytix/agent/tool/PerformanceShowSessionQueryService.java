package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.PerformanceRemoteService;
import com.wimone.enjoytix.agent.remote.dto.PerformanceDetailResponse;
import com.wimone.enjoytix.agent.remote.dto.PerformancePageResponse;
import com.wimone.enjoytix.agent.remote.dto.ShowSessionResponse;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PerformanceShowSessionQueryService implements ShowSessionQueryService {
    private static final int ENABLED = 1;
    private static final long MAX_PAGE_SIZE = 200;

    private final PerformanceRemoteService performanceRemoteService;

    public PerformanceShowSessionQueryService(PerformanceRemoteService performanceRemoteService) {
        this.performanceRemoteService = performanceRemoteService;
    }

    @Override
    public List<ShowSessionRecord> query(String city, LocalDate date, String keyword) {
        try {
            PageResponse<PerformancePageResponse.PerformanceSummary> pageResponse = performanceRemoteService
                    .page(keyword, date, ENABLED, 1, MAX_PAGE_SIZE)
                    .getData();
            if (pageResponse == null || pageResponse.getRecords() == null) {
                return List.of();
            }
            return pageResponse.getRecords().stream()
                    .filter(Objects::nonNull)
                    .filter(summary -> matchesCity(summary.city(), city))
                    .flatMap(summary -> sessions(summary, date, keyword))
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private java.util.stream.Stream<ShowSessionRecord> sessions(
            PerformancePageResponse.PerformanceSummary summary, LocalDate date, String keyword) {
        try {
            PerformanceDetailResponse detail = performanceRemoteService.detail(summary.performanceId()).getData();
            if (detail == null || detail.sessions() == null) {
                return java.util.stream.Stream.empty();
            }
            return detail.sessions().stream()
                    .filter(Objects::nonNull)
                    .filter(session -> session.showTime() != null)
                    .filter(session -> date == null || date.equals(session.showTime().toLocalDate()))
                    .filter(session -> matchesKeyword(detail.title(), keyword))
                    .map(session -> new ShowSessionRecord(
                            session.showId(),
                            firstNonBlank(detail.city(), summary.city()),
                            session.showTime(),
                            firstNonBlank(detail.title(), summary.title())));
        } catch (RuntimeException ex) {
            return java.util.stream.Stream.empty();
        }
    }

    private boolean matchesCity(String actualCity, String requestedCity) {
        return requestedCity == null || requestedCity.isBlank()
                || (actualCity != null && requestedCity.equalsIgnoreCase(actualCity));
    }

    private boolean matchesKeyword(String title, String keyword) {
        return keyword == null || keyword.isBlank()
                || (title != null && title.toLowerCase().contains(keyword.toLowerCase()));
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}