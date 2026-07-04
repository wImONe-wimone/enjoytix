package com.wimone.enjoytix.performance.dto.resp;

import java.time.LocalDateTime;

public record ShowSessionRespDTO(
        Long showId,
        Long performanceId,
        Long hallId,
        LocalDateTime showTime,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        Integer status) {
}
