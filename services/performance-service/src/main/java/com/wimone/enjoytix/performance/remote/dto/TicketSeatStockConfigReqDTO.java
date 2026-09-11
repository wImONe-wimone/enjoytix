package com.wimone.enjoytix.performance.remote.dto;

public record TicketSeatStockConfigReqDTO(
        Long seatId,
        Long areaId,
        Integer rowNo,
        Integer columnNo,
        String seatNo,
        Boolean locked) {
}
