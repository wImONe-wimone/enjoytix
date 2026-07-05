package com.wimone.enjoytix.ticket.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ticket issue response.")
public record TicketIssueRespDTO(
        @Schema(description = "Ticket lock id.", example = "5001")
        Long lockId,
        @Schema(description = "Order id.", example = "6001")
        Long orderId,
        @Schema(description = "Issued electronic ticket codes.")
        List<String> ticketCodes) {
}
