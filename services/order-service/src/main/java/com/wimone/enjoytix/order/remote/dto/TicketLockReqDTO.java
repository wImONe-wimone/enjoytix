package com.wimone.enjoytix.order.remote.dto;

import java.util.List;

public record TicketLockReqDTO(Long showId, Long categoryId, Integer quantity, List<Long> seatIds) {
}
