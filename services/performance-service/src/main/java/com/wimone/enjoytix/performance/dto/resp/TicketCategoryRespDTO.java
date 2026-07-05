package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Ticket category response.")
public record TicketCategoryRespDTO(
        @Schema(description = "Ticket category id.", example = "3001")
        Long categoryId,
        @Schema(description = "Show session id.", example = "2001")
        Long showId,
        @Schema(description = "Ticket category name.", example = "VIP")
        String categoryName,
        @Schema(description = "Ticket price.", example = "1280.00")
        BigDecimal price,
        @Schema(description = "Total stock.", example = "1000")
        Integer totalStock,
        @Schema(description = "Remaining stock.", example = "980")
        Integer remainingStock,
        @Schema(description = "Whether seat selection is enabled, 1 means enabled.", example = "1")
        Integer seatSelectable,
        @Schema(description = "Ticket category status.", example = "1")
        Integer status) {
}
