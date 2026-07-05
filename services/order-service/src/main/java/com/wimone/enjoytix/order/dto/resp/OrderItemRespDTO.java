package com.wimone.enjoytix.order.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Order item response.")
public record OrderItemRespDTO(
        @Schema(description = "Order item id.", example = "6101")
        Long itemId,
        @Schema(description = "Show session id.", example = "2001")
        Long showId,
        @Schema(description = "Ticket category id.", example = "3001")
        Long categoryId,
        @Schema(description = "Ticket quantity.", example = "1")
        Integer quantity,
        @Schema(description = "Selected seat ids.")
        List<Long> seatIds,
        @Schema(description = "Unit ticket price.", example = "1280.00")
        BigDecimal unitPrice,
        @Schema(description = "Item amount.", example = "1280.00")
        BigDecimal amount,
        @Schema(description = "Issued electronic ticket codes.")
        List<String> ticketCodes) {
}
