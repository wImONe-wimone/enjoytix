package com.wimone.enjoytix.ticket.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Ticket availability response.")
public record TicketAvailabilityRespDTO(
        @Schema(description = "Show session id.", example = "2001")
        Long showId,
        @Schema(description = "Ticket category id.", example = "3001")
        Long categoryId,
        @Schema(description = "Ticket category name.", example = "VIP")
        String categoryName,
        @Schema(description = "Ticket price.", example = "1280.00")
        BigDecimal price,
        @Schema(description = "Total stock.", example = "1000")
        Integer totalStock,
        @Schema(description = "Locked stock.", example = "10")
        Integer lockedStock,
        @Schema(description = "Sold stock.", example = "20")
        Integer soldStock,
        @Schema(description = "Available stock.", example = "970")
        Integer availableStock,
        @Schema(description = "Whether seat selection is enabled, 1 means enabled.", example = "1")
        Integer seatSelectable) {
}
