package com.wimone.enjoytix.performance.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.ShowSessionRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryRespDTO;
import com.wimone.enjoytix.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/show")
@Tag(name = "Show API", description = "Show session, ticket category, and seat map APIs.")
public class ShowController {

    private final PerformanceService performanceService;

    public ShowController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Operation(summary = "Get show session", description = "Query show session schedule and sale window.")
    @GetMapping("/{showId}")
    public Result<ShowSessionRespDTO> show(
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId) {
        return Results.success(performanceService.show(showId));
    }

    @Operation(summary = "List ticket categories", description = "List ticket categories and stock snapshot for a show session.")
    @GetMapping("/{showId}/ticket-categories")
    public Result<List<TicketCategoryRespDTO>> ticketCategories(
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId) {
        return Results.success(performanceService.ticketCategories(showId));
    }

    @Operation(summary = "Get seat map", description = "Query the seat map for a show session.")
    @GetMapping("/{showId}/seat-map")
    public Result<SeatMapRespDTO> seatMap(
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId) {
        return Results.success(performanceService.seatMap(showId));
    }
}
