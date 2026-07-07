package com.wimone.enjoytix.performance.remote.dto;

import java.util.List;

public record TicketShowStockInitReqDTO(
        Long sourceShowId,
        Long targetShowId,
        List<TicketCategoryMappingReqDTO> categoryMappings) {
}
