package com.wimone.enjoytix.pay.remote.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailRespDTO(
        Long orderId,
        String orderSn,
        Long userId,
        Long showId,
        Long lockId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime payExpireTime,
        List<OrderItemRespDTO> items) {
}
