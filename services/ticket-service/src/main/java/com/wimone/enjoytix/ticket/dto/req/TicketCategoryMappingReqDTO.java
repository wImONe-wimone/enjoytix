package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.constraints.NotNull;

public record TicketCategoryMappingReqDTO(
        @NotNull Long sourceCategoryId,
        @NotNull Long targetCategoryId) {
}
