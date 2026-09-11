package com.wimone.enjoytix.order.remote.dto;

import com.wimone.enjoytix.framework.base.ticket.TicketAllocationModeEnum;

import java.util.List;

public record TicketLockReqDTO(
        Long showId,
        Long categoryId,
        Long areaId,
        TicketAllocationModeEnum allocationMode,
        Integer quantity,
        List<Long> seatIds) {
}
