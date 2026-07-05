package com.wimone.enjoytix.pay.dto.resp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayRespDTO(
        Long payId,
        String paySn,
        Long orderId,
        BigDecimal amount,
        String status,
        String mockPayUrl,
        LocalDateTime paidTime) {
}
