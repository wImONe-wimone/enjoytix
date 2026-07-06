package com.wimone.enjoytix.order.remote;

import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.order.remote.dto.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.order.remote.dto.TicketIssueReqDTO;
import com.wimone.enjoytix.order.remote.dto.TicketIssueRespDTO;
import com.wimone.enjoytix.order.remote.dto.TicketLockReqDTO;
import com.wimone.enjoytix.order.remote.dto.TicketLockRespDTO;
import com.wimone.enjoytix.order.remote.dto.TicketReleaseReqDTO;
import com.wimone.enjoytix.order.remote.dto.TicketRefundReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "enjoytix-ticket-service")
public interface TicketRemoteService {

    @GetMapping("/api/ticket/availability")
    Result<List<TicketAvailabilityRespDTO>> availability(@RequestParam("showId") Long showId);

    @PostMapping("/api/ticket/lock")
    Result<TicketLockRespDTO> lock(@RequestHeader("X-User-Id") Long userId, @RequestBody TicketLockReqDTO requestParam);

    @PostMapping("/api/ticket/release")
    Result<Boolean> release(@RequestHeader("X-User-Id") Long userId, @RequestBody TicketReleaseReqDTO requestParam);

    @PostMapping("/api/ticket/issue")
    Result<TicketIssueRespDTO> issue(@RequestHeader("X-User-Id") Long userId, @RequestBody TicketIssueReqDTO requestParam);

    @PostMapping("/api/ticket/refund")
    Result<Boolean> refund(@RequestHeader("X-User-Id") Long userId, @RequestBody TicketRefundReqDTO requestParam);
}
