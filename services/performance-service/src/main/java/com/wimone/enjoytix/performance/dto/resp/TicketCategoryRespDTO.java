package com.wimone.enjoytix.performance.dto.resp;

import java.math.BigDecimal;

public record TicketCategoryRespDTO(
        Long categoryId,
        Long showId,
        String categoryName,
        BigDecimal price,
        Integer totalStock,
        Integer remainingStock,
        Integer seatSelectable,
        Integer status) {
}
