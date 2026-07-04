package com.wimone.enjoytix.performance.dto.resp;

import java.util.List;

public record SeatMapRespDTO(
        Long seatMapId,
        String name,
        Integer rowCount,
        Integer columnCount,
        List<SeatRespDTO> seats) {
}
