package com.wimone.enjoytix.pay.dto.resp;

import java.math.BigDecimal;

public record RefundRespDTO(
        Long refundId,
        Long payId,
        Long orderId,
        BigDecimal amount,
        String status,
        String reason) {
}
