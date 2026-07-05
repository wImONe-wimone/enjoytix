package com.wimone.enjoytix.order.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Order creation response.")
public record OrderCreateRespDTO(
        @Schema(description = "Order id.", example = "6001")
        Long orderId,
        @Schema(description = "Order serial number.", example = "EO6001")
        String orderSn,
        @Schema(description = "Ticket lock id.", example = "5001")
        Long lockId,
        @Schema(description = "Total order amount.", example = "1280.00")
        BigDecimal totalAmount,
        @Schema(description = "Order status.", example = "PENDING_PAYMENT")
        String status,
        @Schema(description = "Payment expiration time.", example = "2026-08-01T19:45:00")
        LocalDateTime payExpireTime) {
}
