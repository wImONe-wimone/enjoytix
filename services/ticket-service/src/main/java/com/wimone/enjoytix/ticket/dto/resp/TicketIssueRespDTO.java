package com.wimone.enjoytix.ticket.dto.resp;

import java.util.List;

public record TicketIssueRespDTO(Long lockId, Long orderId, List<String> ticketCodes) {
}
