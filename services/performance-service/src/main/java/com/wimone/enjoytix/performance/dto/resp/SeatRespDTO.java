package com.wimone.enjoytix.performance.dto.resp;

public record SeatRespDTO(
        Long seatId,
        String areaName,
        Integer rowNo,
        Integer columnNo,
        String seatNo,
        Integer status) {
}
