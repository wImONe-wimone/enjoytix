package com.wimone.enjoytix.agent.remote;

import com.wimone.enjoytix.agent.remote.dto.PerformanceDetailResponse;
import com.wimone.enjoytix.agent.remote.dto.PerformancePageResponse;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "enjoytix-performance-service")
public interface PerformanceRemoteService {

    @GetMapping("/api/performance/page")
    Result<PageResponse<PerformancePageResponse.PerformanceSummary>> page(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "showDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "current", defaultValue = "1") long current,
            @RequestParam(value = "size", defaultValue = "200") long size);

    @GetMapping("/api/performance/{performanceId}")
    Result<PerformanceDetailResponse> detail(@PathVariable("performanceId") Long performanceId);
}