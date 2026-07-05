package com.wimone.enjoytix.pay.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Refund response.")
public record RefundRespDTO(
        @Schema(description = "Refund order id.", example = "8001")
        Long refundId,
        @Schema(description = "Pay order id.", example = "7001")
        Long payId,
        @Schema(description = "Order id.", example = "6001")
        Long orderId,
        @Schema(description = "Refund amount.", example = "1280.00")
        BigDecimal amount,
        @Schema(description = "Refund status.", example = "SUCCESS")
        String status,
        @Schema(description = "Refund reason.", example = "Customer request")
        String reason) {
}
