package com.wimone.enjoytix.performance.remote.dto;

import java.math.BigDecimal;
import java.util.List;

public record TicketCategoryStockConfigReqDTO(
        Long categoryId,
        String categoryName,
        BigDecimal price,
        Integer totalStock,
        Integer seatSelectable,
        List<TicketSeatStockConfigReqDTO> seats) {
}
