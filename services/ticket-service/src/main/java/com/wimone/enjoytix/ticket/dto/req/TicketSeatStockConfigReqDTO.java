package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.constraints.NotNull;

public record TicketSeatStockConfigReqDTO(
        @NotNull Long seatId,
        @NotNull Long areaId,
        @NotNull Integer rowNo,
        @NotNull Integer columnNo,
        @jakarta.validation.constraints.NotBlank String seatNo,
        Boolean locked) {
}
