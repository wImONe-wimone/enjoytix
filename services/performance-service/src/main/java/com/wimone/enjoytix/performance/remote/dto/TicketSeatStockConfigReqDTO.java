package com.wimone.enjoytix.performance.remote.dto;

public record TicketSeatStockConfigReqDTO(
        Long seatId,
        String areaName,
        Integer rowNo,
        Integer columnNo,
        String seatNo,
        Boolean locked) {
}
