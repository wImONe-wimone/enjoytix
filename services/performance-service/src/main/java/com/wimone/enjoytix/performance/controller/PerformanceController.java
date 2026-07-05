package com.wimone.enjoytix.performance.controller;

import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance API", description = "Performance search and detail APIs.")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Operation(summary = "Page performances", description = "Search performances by city, type, artist, venue, and show date.")
    @GetMapping("/page")
    public Result<PageResponse<PerformanceListRespDTO>> page(@Valid @ModelAttribute PerformancePageQueryReqDTO requestParam) {
        return Results.success(performanceService.pageQuery(requestParam));
    }

    @Operation(summary = "Get performance detail", description = "Query performance detail, venue, artist, and show sessions.")
    @GetMapping("/{performanceId}")
    public Result<PerformanceDetailRespDTO> detail(
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(performanceService.detail(performanceId));
    }
}
