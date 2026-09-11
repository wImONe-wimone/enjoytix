package com.wimone.enjoytix.order.remote.dto;

import java.time.LocalDateTime;

public record ShowSessionRespDTO(
        Long showId,
        Long performanceId,
        Long hallId,
        LocalDateTime showTime,
        Integer durationMinutes,
        LocalDateTime saleStartTime,
        LocalDateTime saleEndTime,
        Integer status) {
}
