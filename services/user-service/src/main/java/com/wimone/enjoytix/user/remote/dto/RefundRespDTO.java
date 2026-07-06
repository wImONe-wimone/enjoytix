package com.wimone.enjoytix.user.remote.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

public record RefundRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long refundId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long payId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long orderId,
        BigDecimal amount,
        String status,
        String reason) {
}
