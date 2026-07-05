package com.wimone.enjoytix.ticket.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Ticket lock response.")
public record TicketLockRespDTO(
        @Schema(description = "Ticket lock id.", example = "5001")
        Long lockId,
        @Schema(description = "Show session id.", example = "2001")
        Long showId,
        @Schema(description = "Ticket category id.", example = "3001")
        Long categoryId,
        @Schema(description = "Locked ticket quantity.", example = "1")
        Integer quantity,
        @Schema(description = "Locked seat ids.")
        List<Long> seatIds,
        @Schema(description = "Lock expiration time.", example = "2026-08-01T19:45:00")
        LocalDateTime expireTime) {
}
