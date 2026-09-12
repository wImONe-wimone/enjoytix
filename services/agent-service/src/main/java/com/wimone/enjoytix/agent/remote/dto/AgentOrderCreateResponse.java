package com.wimone.enjoytix.agent.remote.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgentOrderCreateResponse(
        Long orderId,
        String orderSn,
        Long lockId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime payExpireTime) {
}
