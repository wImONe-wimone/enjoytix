package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TicketShowStockConfigInitReqDTO(
        @NotNull Long showId,
        @NotEmpty List<@Valid TicketCategoryStockConfigReqDTO> categories) {
}
