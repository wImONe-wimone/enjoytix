package com.wimone.enjoytix.performance.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.ShowSessionRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryRespDTO;
import com.wimone.enjoytix.performance.service.PerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/show")
public class ShowController {

    private final PerformanceService performanceService;

    public ShowController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/{showId}")
    public Result<ShowSessionRespDTO> show(@PathVariable Long showId) {
        return Results.success(performanceService.show(showId));
    }

    @GetMapping("/{showId}/ticket-categories")
    public Result<List<TicketCategoryRespDTO>> ticketCategories(@PathVariable Long showId) {
        return Results.success(performanceService.ticketCategories(showId));
    }

    @GetMapping("/{showId}/seat-map")
    public Result<SeatMapRespDTO> seatMap(@PathVariable Long showId) {
        return Results.success(performanceService.seatMap(showId));
    }
}
