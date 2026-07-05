package com.wimone.enjoytix.order.remote.dto;

import java.util.List;

public record TicketIssueRespDTO(Long lockId, Long orderId, List<String> ticketCodes) {
}
