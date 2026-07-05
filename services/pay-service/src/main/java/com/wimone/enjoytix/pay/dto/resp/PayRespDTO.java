package com.wimone.enjoytix.pay.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment order response.")
public record PayRespDTO(
        @Schema(description = "Pay order id.", example = "7001")
        Long payId,
        @Schema(description = "Pay serial number.", example = "EP7001")
        String paySn,
        @Schema(description = "Order id.", example = "6001")
        Long orderId,
        @Schema(description = "Payment amount.", example = "1280.00")
        BigDecimal amount,
        @Schema(description = "Payment status.", example = "WAITING")
        String status,
        @Schema(description = "Mock payment URL.")
        String mockPayUrl,
        @Schema(description = "Paid time.", example = "2026-08-01T19:40:00")
        LocalDateTime paidTime) {
}
