package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.PerformanceRemoteService;
import com.wimone.enjoytix.agent.remote.dto.PerformanceDetailResponse;
import com.wimone.enjoytix.agent.remote.dto.PerformancePageResponse;
import com.wimone.enjoytix.agent.remote.dto.ShowSessionResponse;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceShowSessionQueryServiceTest {

    @Test
    void queriesPerformancePageAndExpandsSessions() {
        PerformanceRemoteService remote = new StubPerformanceRemoteService();
        PerformanceShowSessionQueryService service = new PerformanceShowSessionQueryService(remote);

        List<ShowSessionRecord> records = service.query("Shanghai", LocalDate.of(2026, 10, 1), "Jazz");

        assertThat(records).containsExactly(new ShowSessionRecord(
                2001L, "Shanghai", LocalDateTime.of(2026, 10, 1, 19, 30), "Jazz Night"));
    }

    @Test
    void returnsEmptyListWhenRemoteQueryFails() {
        PerformanceRemoteService remote = new PerformanceRemoteService() {
            @Override
            public Result<PageResponse<PerformancePageResponse.PerformanceSummary>> page(String title, LocalDate showDate, Integer status, long current, long size) {
                return Result.failure(new com.wimone.enjoytix.framework.convention.errorcode.IErrorCode() {
                    public String code() { return "REMOTE_ERROR"; }
                    public String message() { return "remote error"; }
                });
            }

            @Override
            public Result<PerformanceDetailResponse> detail(Long performanceId) {
                return Result.success(null);
            }
        };

        assertThat(new PerformanceShowSessionQueryService(remote)
                .query(null, null, null)).isEmpty();
    }

    private static class StubPerformanceRemoteService implements PerformanceRemoteService {
        @Override
        public Result<PageResponse<PerformancePageResponse.PerformanceSummary>> page(String title, LocalDate showDate, Integer status, long current, long size) {
            return Result.success(new PageResponse<>(1, 200, 1, List.of(
                    new PerformancePageResponse.PerformanceSummary(1001L, "Jazz Night", "Shanghai"))));
        }

        @Override
        public Result<PerformanceDetailResponse> detail(Long performanceId) {
            return Result.success(new PerformanceDetailResponse(1001L, "Jazz Night", "Shanghai", List.of(
                    new ShowSessionResponse(2001L, LocalDateTime.of(2026, 10, 1, 19, 30)),
                    new ShowSessionResponse(2002L, LocalDateTime.of(2026, 10, 2, 19, 30)))));
        }
    }
}