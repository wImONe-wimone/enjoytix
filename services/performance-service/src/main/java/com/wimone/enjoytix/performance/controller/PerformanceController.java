package com.wimone.enjoytix.performance.controller;

import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.performance.dto.req.ArtistCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.ArtistUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformanceCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformancePageQueryReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformanceSaleScheduleReqDTO;
import com.wimone.enjoytix.performance.dto.req.PerformanceUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.SeatCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.SeatUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.ShowSessionCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.ShowSessionUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.req.TicketCategoryConfigReqDTO;
import com.wimone.enjoytix.performance.dto.req.VenueCreateReqDTO;
import com.wimone.enjoytix.performance.dto.req.VenueUpdateReqDTO;
import com.wimone.enjoytix.performance.dto.resp.ArtistRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceDetailRespDTO;
import com.wimone.enjoytix.performance.dto.resp.PerformanceListRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatMapRespDTO;
import com.wimone.enjoytix.performance.dto.resp.SeatRespDTO;
import com.wimone.enjoytix.performance.dto.resp.ShowSessionRespDTO;
import com.wimone.enjoytix.performance.dto.resp.TicketCategoryConfigRespDTO;
import com.wimone.enjoytix.performance.dto.resp.VenueRespDTO;
import com.wimone.enjoytix.performance.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance API", description = "Performance search and detail APIs.")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @Operation(summary = "Page performances", description = "Search performances by type, artist, venue, and show date.")
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

    @Operation(summary = "Admin page performances", description = "Admin performance list with the same search conditions as public page API.")
    @GetMapping("/admin/page")
    public Result<PageResponse<PerformanceListRespDTO>> adminPage(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @ModelAttribute PerformancePageQueryReqDTO requestParam) {
        return Results.success(performanceService.pageQuery(requestParam));
    }

    @Operation(summary = "Admin get performance detail", description = "Admin query performance detail for editing.")
    @GetMapping("/admin/{performanceId}")
    public Result<PerformanceDetailRespDTO> adminDetail(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(performanceService.detail(performanceId));
    }

    @Operation(summary = "Admin create performance", description = "Create a performance project.")
    @OperationLog("admin-performance-create")
    @PostMapping("/admin")
    public Result<PerformanceDetailRespDTO> create(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody PerformanceCreateReqDTO requestParam) {
        return Results.success(performanceService.create(operatorId, requestParam));
    }

    @Operation(summary = "Admin update performance", description = "Update a performance project.")
    @OperationLog("admin-performance-update")
    @PutMapping("/admin/{performanceId}")
    public Result<PerformanceDetailRespDTO> update(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Valid @RequestBody PerformanceUpdateReqDTO requestParam) {
        return Results.success(performanceService.update(performanceId, requestParam));
    }

    @Operation(summary = "Admin delete performance", description = "Soft delete a performance project.")
    @OperationLog("admin-performance-delete")
    @DeleteMapping("/admin/{performanceId}")
    public Result<Boolean> delete(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(performanceService.delete(performanceId));
    }

    @Operation(summary = "Admin start performance sale now", description = "Start selling a performance project immediately after ticket category readiness validation.")
    @OperationLog("admin-performance-sale-start-now")
    @PostMapping("/admin/{performanceId}/sale/start-now")
    public Result<PerformanceDetailRespDTO> startSaleNow(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(performanceService.startSaleNow(operatorId, performanceId));
    }

    @Operation(summary = "Admin schedule performance sale", description = "Schedule a performance project sale start through delayed message queue.")
    @OperationLog("admin-performance-sale-schedule")
    @PostMapping("/admin/{performanceId}/sale/schedule")
    public Result<PerformanceDetailRespDTO> scheduleSale(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Valid @RequestBody PerformanceSaleScheduleReqDTO requestParam) {
        return Results.success(performanceService.scheduleSale(operatorId, performanceId, requestParam));
    }

    @Operation(summary = "Admin list show sessions", description = "List show sessions under a performance project.")
    @GetMapping("/admin/{performanceId}/shows")
    public Result<List<ShowSessionRespDTO>> adminShows(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(performanceService.adminShows(performanceId));
    }

    @Operation(summary = "Admin create show session", description = "Create a show session under a performance project.")
    @OperationLog("admin-show-session-create")
    @PostMapping("/admin/{performanceId}/shows")
    public Result<ShowSessionRespDTO> createShow(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Valid @RequestBody ShowSessionCreateReqDTO requestParam) {
        return Results.success(performanceService.createShow(operatorId, performanceId, requestParam));
    }

    @Operation(summary = "Admin update show session", description = "Update a show session under a performance project.")
    @OperationLog("admin-show-session-update")
    @PutMapping("/admin/{performanceId}/shows/{showId}")
    public Result<ShowSessionRespDTO> updateShow(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId,
            @Valid @RequestBody ShowSessionUpdateReqDTO requestParam) {
        return Results.success(performanceService.updateShow(performanceId, showId, requestParam));
    }

    @Operation(summary = "Admin delete show session", description = "Soft delete a show session under a performance project.")
    @OperationLog("admin-show-session-delete")
    @DeleteMapping("/admin/{performanceId}/shows/{showId}")
    public Result<Boolean> deleteShow(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId) {
        return Results.success(performanceService.deleteShow(performanceId, showId));
    }

    @Operation(summary = "Admin get show ticket category config", description = "Query ticket categories and seat assignments under a show session.")
    @GetMapping("/admin/{performanceId}/shows/{showId}/ticket-categories/config")
    public Result<List<TicketCategoryConfigRespDTO>> ticketCategoryConfig(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId) {
        return Results.success(performanceService.ticketCategoryConfig(showId));
    }

    @Operation(summary = "Admin configure show ticket categories", description = "Configure ticket categories and seat assignments for an empty show session.")
    @OperationLog("admin-show-ticket-category-config")
    @PutMapping("/admin/{performanceId}/shows/{showId}/ticket-categories/config")
    public Result<List<TicketCategoryConfigRespDTO>> configureTicketCategories(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Parameter(description = "Show session id.", required = true)
            @PathVariable Long showId,
            @Valid @RequestBody List<@Valid TicketCategoryConfigReqDTO> requestParam) {
        return Results.success(performanceService.configureShowTicketCategories(operatorId, performanceId, showId, requestParam));
    }

    @Operation(summary = "Admin list artists", description = "List artists for performance form options.")
    @GetMapping("/admin/artists")
    public Result<List<ArtistRespDTO>> artists(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId) {
        return Results.success(performanceService.artists());
    }

    @Operation(summary = "Admin get artist", description = "Query artist or troupe detail.")
    @GetMapping("/admin/artists/{artistId}")
    public Result<ArtistRespDTO> artist(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Artist id.", required = true)
            @PathVariable Long artistId) {
        return Results.success(performanceService.artist(artistId));
    }

    @Operation(summary = "Admin create artist", description = "Create an artist or troupe.")
    @OperationLog("admin-artist-create")
    @PostMapping("/admin/artists")
    public Result<ArtistRespDTO> createArtist(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody ArtistCreateReqDTO requestParam) {
        return Results.success(performanceService.createArtist(requestParam));
    }

    @Operation(summary = "Admin update artist", description = "Update an artist or troupe.")
    @OperationLog("admin-artist-update")
    @PutMapping("/admin/artists/{artistId}")
    public Result<ArtistRespDTO> updateArtist(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Artist id.", required = true)
            @PathVariable Long artistId,
            @Valid @RequestBody ArtistUpdateReqDTO requestParam) {
        return Results.success(performanceService.updateArtist(artistId, requestParam));
    }

    @Operation(summary = "Admin delete artist", description = "Soft delete an artist or troupe if it is not referenced by performances.")
    @OperationLog("admin-artist-delete")
    @DeleteMapping("/admin/artists/{artistId}")
    public Result<Boolean> deleteArtist(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Artist id.", required = true)
            @PathVariable Long artistId) {
        return Results.success(performanceService.deleteArtist(artistId));
    }

    @Operation(summary = "Admin list venues", description = "List venues for performance form options.")
    @GetMapping("/admin/venues")
    public Result<List<VenueRespDTO>> venues(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId) {
        return Results.success(performanceService.venues());
    }

    @Operation(summary = "Admin get venue", description = "Query venue detail.")
    @GetMapping("/admin/venues/{venueId}")
    public Result<VenueRespDTO> venue(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId) {
        return Results.success(performanceService.venue(venueId));
    }

    @Operation(summary = "Admin create venue", description = "Create a venue and initialize a default hall and seat map.")
    @OperationLog("admin-venue-create")
    @PostMapping("/admin/venues")
    public Result<VenueRespDTO> createVenue(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody VenueCreateReqDTO requestParam) {
        return Results.success(performanceService.createVenue(requestParam));
    }

    @Operation(summary = "Admin update venue", description = "Update venue base information.")
    @OperationLog("admin-venue-update")
    @PutMapping("/admin/venues/{venueId}")
    public Result<VenueRespDTO> updateVenue(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId,
            @Valid @RequestBody VenueUpdateReqDTO requestParam) {
        return Results.success(performanceService.updateVenue(venueId, requestParam));
    }

    @Operation(summary = "Admin delete venue", description = "Soft delete a venue if it is not referenced by performances.")
    @OperationLog("admin-venue-delete")
    @DeleteMapping("/admin/venues/{venueId}")
    public Result<Boolean> deleteVenue(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId) {
        return Results.success(performanceService.deleteVenue(venueId));
    }

    @Operation(summary = "Admin list venue seats", description = "List physical seats under the default hall of a venue.")
    @GetMapping("/admin/venues/{venueId}/seats")
    public Result<List<SeatRespDTO>> venueSeats(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId) {
        return Results.success(performanceService.venueSeats(venueId));
    }

    @Operation(summary = "Admin get venue seat map", description = "Get the default hall seat map of a venue.")
    @GetMapping("/admin/venues/{venueId}/seat-map")
    public Result<SeatMapRespDTO> venueSeatMap(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId) {
        return Results.success(performanceService.venueSeatMap(venueId));
    }

    @Operation(summary = "Admin create venue seat", description = "Create a physical seat under the default hall of a venue.")
    @OperationLog("admin-venue-seat-create")
    @PostMapping("/admin/venues/{venueId}/seats")
    public Result<SeatRespDTO> createVenueSeat(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId,
            @Valid @RequestBody SeatCreateReqDTO requestParam) {
        return Results.success(performanceService.createVenueSeat(venueId, requestParam));
    }

    @Operation(summary = "Admin update venue seat", description = "Update a physical seat if it has not been assigned to any show ticket category.")
    @OperationLog("admin-venue-seat-update")
    @PutMapping("/admin/venues/{venueId}/seats/{seatId}")
    public Result<SeatRespDTO> updateVenueSeat(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId,
            @Parameter(description = "Seat id.", required = true)
            @PathVariable Long seatId,
            @Valid @RequestBody SeatUpdateReqDTO requestParam) {
        return Results.success(performanceService.updateVenueSeat(venueId, seatId, requestParam));
    }

    @Operation(summary = "Admin delete venue seat", description = "Soft delete a physical seat if it has not been assigned to any show ticket category.")
    @OperationLog("admin-venue-seat-delete")
    @DeleteMapping("/admin/venues/{venueId}/seats/{seatId}")
    public Result<Boolean> deleteVenueSeat(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader("X-User-Id") Long operatorId,
            @Parameter(description = "Venue id.", required = true)
            @PathVariable Long venueId,
            @Parameter(description = "Seat id.", required = true)
            @PathVariable Long seatId) {
        return Results.success(performanceService.deleteVenueSeat(venueId, seatId));
    }
}
