package com.wimone.enjoytix.user.remote.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long orderId,
        String orderSn,
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long showId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long lockId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime payExpireTime,
        List<OrderItemRespDTO> items) {
}
