package com.wimone.enjoytix.ticket.dto.resp;

public record SeatAvailabilityRespDTO(
        Long showId,
        Long categoryId,
        Long seatId,
        String areaName,
        Integer rowNo,
        Integer columnNo,
        String seatNo,
        String status) {
}
