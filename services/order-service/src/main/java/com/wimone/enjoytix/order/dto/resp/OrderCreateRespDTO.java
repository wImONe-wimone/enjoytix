package com.wimone.enjoytix.order.dto.resp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreateRespDTO(
        Long orderId,
        String orderSn,
        Long lockId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime payExpireTime) {
}
