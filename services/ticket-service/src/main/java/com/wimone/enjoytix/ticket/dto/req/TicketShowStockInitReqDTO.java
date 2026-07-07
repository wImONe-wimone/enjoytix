package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TicketShowStockInitReqDTO(
        @NotNull Long sourceShowId,
        @NotNull Long targetShowId,
        @NotEmpty List<@Valid TicketCategoryMappingReqDTO> categoryMappings) {
}
