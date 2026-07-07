package com.wimone.enjoytix.ticket.controller;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.idempotent.annotation.Idempotent;
import com.wimone.enjoytix.framework.idempotent.enums.IdempotentTypeEnum;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import com.wimone.enjoytix.ticket.common.TicketConstants;
import com.wimone.enjoytix.ticket.dto.req.TicketIssueReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketLockReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketReleaseReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketRefundReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketShowStockConfigInitReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketShowStockInitReqDTO;
import com.wimone.enjoytix.ticket.dto.resp.SeatAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketIssueRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketLockRespDTO;
import com.wimone.enjoytix.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/ticket")
@Tag(name = "Ticket API", description = "Ticket inventory, seat availability, lock, release, and issue APIs.")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 查询某场次的票档库存
     */
    @Operation(summary = "List ticket availability", description = "Query ticket category stock for a show session.")
    @GetMapping("/availability")
    public Result<List<TicketAvailabilityRespDTO>> availability(
            @Parameter(description = "Show session id.", required = true)
            @NotNull @RequestParam Long showId) {
        return Results.success(ticketService.availability(showId));
    }

    /**
     * 查询某场次的座位状态
     */
    @Operation(summary = "List seat availability", description = "Query seat-level availability for a show session.")
    @GetMapping("/seats")
    public Result<List<SeatAvailabilityRespDTO>> seats(
            @Parameter(description = "Show session id.", required = true)
            @NotNull @RequestParam Long showId) {
        return Results.success(ticketService.seats(showId));
    }

    // 是锁定票的库存和座位
    @OperationLog("ticket-lock")
    @Operation(summary = "Lock tickets", description = "Lock ticket category inventory or specific seats for checkout.")
    @Idempotent(
            key = "'ticket:lock:' + #p0 + ':' + #p1.showId + ':' + #p1.categoryId + ':' + #p1.quantity + ':' + #p1.seatIds",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 5,
            message = "Ticket lock request is being processed"
    )
    @PostMapping("/lock")
    public Result<TicketLockRespDTO> lock(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketLockReqDTO requestParam) {
        return Results.success(ticketService.lock(userId, requestParam));
    }

    // 释放锁票
    @OperationLog("ticket-release")
    @Operation(summary = "Release ticket lock", description = "Release a pending ticket lock and return stock or seats.")
    @Idempotent(
            key = "'ticket:release:' + #p0 + ':' + #p1.lockId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Ticket release request is being processed"
    )
    @PostMapping("/release")
    public Result<Boolean> release(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketReleaseReqDTO requestParam) {
        return Results.success(ticketService.release(userId, requestParam));
    }

    // 出票
    @OperationLog("ticket-issue")
    @Operation(summary = "Issue tickets", description = "Confirm a locked ticket stock or seat lock and generate ticket codes.")
    @Idempotent(
            key = "'ticket:issue:' + #p0 + ':' + #p1.lockId + ':' + #p1.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Ticket issue request is being processed"
    )
    @PostMapping("/issue")
    public Result<TicketIssueRespDTO> issue(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketIssueReqDTO requestParam) {
        return Results.success(ticketService.issue(userId, requestParam));
    }

    // 閫€绁ㄥ悗閲婃斁宸插嚭绁ㄧ殑搴т綅鍜屽簱瀛?
    @OperationLog("ticket-refund")
    @Operation(summary = "Refund issued tickets", description = "Return issued ticket stock or seats after a successful refund.")
    @Idempotent(
            key = "'ticket:refund:' + #p0 + ':' + #p1.lockId + ':' + #p1.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Ticket refund request is being processed"
    )
    @PostMapping("/refund")
    public Result<Boolean> refund(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketRefundReqDTO requestParam) {
        return Results.success(ticketService.refund(userId, requestParam));
    }

    @OperationLog("ticket-admin-show-stock-init")
    @Operation(summary = "Init show stock", description = "Copy ticket stock and seat stock from one show session to another.")
    @PostMapping("/admin/shows/init-stock")
    public Result<Boolean> initShowStock(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long operatorId,
            @Valid @RequestBody TicketShowStockInitReqDTO requestParam) {
        return Results.success(ticketService.initShowStock(requestParam));
    }

    @OperationLog("ticket-admin-show-stock-config-init")
    @Operation(summary = "Init configured show stock", description = "Initialize ticket stock and seat stock from performance-service ticket category configuration.")
    @PostMapping("/admin/shows/init-stock/config")
    public Result<Boolean> initConfiguredShowStock(
            @Parameter(description = "Current operator user id.", required = true)
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long operatorId,
            @Valid @RequestBody TicketShowStockConfigInitReqDTO requestParam) {
        return Results.success(ticketService.initConfiguredShowStock(requestParam));
    }
}
