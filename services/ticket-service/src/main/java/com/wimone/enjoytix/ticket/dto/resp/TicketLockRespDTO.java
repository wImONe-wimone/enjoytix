package com.wimone.enjoytix.ticket.dto.resp;

import java.time.LocalDateTime;
import java.util.List;

public record TicketLockRespDTO(
        Long lockId,
        Long showId,
        Long categoryId,
        Integer quantity,
        List<Long> seatIds,
        LocalDateTime expireTime) {
}
