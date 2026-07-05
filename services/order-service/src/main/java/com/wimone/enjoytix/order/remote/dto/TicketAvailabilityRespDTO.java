package com.wimone.enjoytix.order.remote.dto;

import java.math.BigDecimal;

public record TicketAvailabilityRespDTO(
        Long showId,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        Integer totalStock,
        Integer lockedStock,
        Integer soldStock,
        Integer availableStock,
        Integer seatSelectable) {
}
