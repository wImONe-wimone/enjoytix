package com.wimone.enjoytix.ticket.service;

import com.wimone.enjoytix.ticket.dto.req.TicketIssueReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketLockReqDTO;
import com.wimone.enjoytix.ticket.dto.req.TicketReleaseReqDTO;
import com.wimone.enjoytix.ticket.dto.resp.SeatAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketAvailabilityRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketIssueRespDTO;
import com.wimone.enjoytix.ticket.dto.resp.TicketLockRespDTO;

import java.util.List;

public interface TicketService {

    List<TicketAvailabilityRespDTO> availability(Long showId);

    List<SeatAvailabilityRespDTO> seats(Long showId);

    TicketLockRespDTO lock(Long userId, TicketLockReqDTO requestParam);

    Boolean release(Long userId, TicketReleaseReqDTO requestParam);

    TicketIssueRespDTO issue(Long userId, TicketIssueReqDTO requestParam);
}
