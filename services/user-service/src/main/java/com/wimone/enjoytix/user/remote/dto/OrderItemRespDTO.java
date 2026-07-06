package com.wimone.enjoytix.user.remote.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long itemId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long showId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long categoryId,
        Integer quantity,
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        List<Long> seatIds,
        BigDecimal unitPrice,
        BigDecimal amount,
        List<String> ticketCodes) {
}
