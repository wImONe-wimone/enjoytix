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
import com.wimone.enjoytix.ticket.dto.resp.SeatAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketIssueRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketLockRespDTO;
import com.wimone.enjoytix.ticket.service.TicketService;
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
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/availability")
    public Result<List<TicketAvailabilityRespDTO>> availability(@NotNull @RequestParam Long showId) {
        return Results.success(ticketService.availability(showId));
    }

    @GetMapping("/seats")
    public Result<List<SeatAvailabilityRespDTO>> seats(@NotNull @RequestParam Long showId) {
        return Results.success(ticketService.seats(showId));
    }

    @OperationLog("ticket-lock")
    @Idempotent(
            key = "'ticket:lock:' + #p0 + ':' + #p1.showId + ':' + #p1.categoryId + ':' + #p1.quantity + ':' + #p1.seatIds",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 5,
            message = "Ticket lock request is being processed"
    )
    @PostMapping("/lock")
    public Result<TicketLockRespDTO> lock(
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketLockReqDTO requestParam) {
        return Results.success(ticketService.lock(userId, requestParam));
    }

    @OperationLog("ticket-release")
    @Idempotent(
            key = "'ticket:release:' + #p0 + ':' + #p1.lockId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Ticket release request is being processed"
    )
    @PostMapping("/release")
    public Result<Boolean> release(
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketReleaseReqDTO requestParam) {
        return Results.success(ticketService.release(userId, requestParam));
    }

    @OperationLog("ticket-issue")
    @Idempotent(
            key = "'ticket:issue:' + #p0 + ':' + #p1.lockId + ':' + #p1.orderId",
            type = IdempotentTypeEnum.SPEL,
            keyTimeout = 10,
            message = "Ticket issue request is being processed"
    )
    @PostMapping("/issue")
    public Result<TicketIssueRespDTO> issue(
            @RequestHeader(TicketConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody TicketIssueReqDTO requestParam) {
        return Results.success(ticketService.issue(userId, requestParam));
    }
}
