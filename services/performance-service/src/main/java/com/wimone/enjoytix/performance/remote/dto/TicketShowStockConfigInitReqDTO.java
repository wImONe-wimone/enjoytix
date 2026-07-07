package com.wimone.enjoytix.performance.remote.dto;

import java.util.List;

public record TicketShowStockConfigInitReqDTO(
        Long showId,
        List<TicketCategoryStockConfigReqDTO> categories) {
}
