package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Show ticket category configuration response.")
public record TicketCategoryConfigRespDTO(
        @Schema(description = "Ticket category id.", example = "3001")
        Long categoryId,
        @Schema(description = "Show id.", example = "2001")
        Long showId,
        @Schema(description = "Ticket category name.", example = "VIP")
        String categoryName,
        @Schema(description = "Ticket price.", example = "1280.00")
        BigDecimal price,
        @Schema(description = "Total stock.", example = "12")
        Integer totalStock,
        @Schema(description = "Remaining stock.", example = "12")
        Integer remainingStock,
        @Schema(description = "Whether seats are selectable.", example = "1")
        Integer seatSelectable,
        @Schema(description = "Deprecated compatibility field, always 1. Seat sale availability is controlled by assigned seats and lockedSeats.", example = "1")
        Integer status,
        @Schema(description = "Seats assigned to this category.")
        List<SeatRespDTO> seats,
        @Schema(description = "Seats locked before sale and unavailable to sell.")
        List<SeatRespDTO> lockedSeats) {
}
