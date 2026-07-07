package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketSeatStockConfigReqDTO(
        @NotNull Long seatId,
        @NotBlank String areaName,
        @NotNull Integer rowNo,
        @NotNull Integer columnNo,
        @NotBlank String seatNo,
        Boolean locked) {
}
