package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record TicketCategoryStockConfigReqDTO(
        @NotNull Long categoryId,
        @NotBlank String categoryName,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull Integer totalStock,
        @NotNull Integer seatSelectable,
        List<@Valid TicketSeatStockConfigReqDTO> seats) {
}
