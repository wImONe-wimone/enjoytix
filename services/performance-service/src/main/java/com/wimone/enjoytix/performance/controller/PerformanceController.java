package com.wimone.enjoytix.performance.controller;

import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.service.PerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/page")
    public Result<PageResponse<PerformanceListRespDTO>> page(@ModelAttribute PerformancePageQueryReqDTO requestParam) {
        return Results.success(performanceService.pageQuery(requestParam));
    }

    @GetMapping("/{performanceId}")
    public Result<PerformanceDetailRespDTO> detail(@PathVariable Long performanceId) {
        return Results.success(performanceService.detail(performanceId));
    }
}
