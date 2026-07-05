package com.wimone.enjoytix.ticket.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seat availability response.")
public record SeatAvailabilityRespDTO(
        @Schema(description = "Show session id.", example = "2001")
        Long showId,
        @Schema(description = "Ticket category id.", example = "3001")
        Long categoryId,
        @Schema(description = "Seat id.", example = "400101")
        Long seatId,
        @Schema(description = "Seat area name.", example = "A Zone")
        String areaName,
        @Schema(description = "Row number.", example = "1")
        Integer rowNo,
        @Schema(description = "Column number.", example = "8")
        Integer columnNo,
        @Schema(description = "Seat number.", example = "A1-08")
        String seatNo,
        @Schema(description = "Seat stock status.", example = "AVAILABLE")
        String status) {
}
