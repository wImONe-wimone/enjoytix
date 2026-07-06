package com.wimone.enjoytix.user.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Current user order refund response.")
public record UserOrderRefundRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Order id.", type = "string", example = "6001")
        Long orderId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Pay order id.", type = "string", example = "7001")
        Long payId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Refund order id.", type = "string", example = "8001")
        Long refundId,
        @Schema(description = "Refund amount.", example = "1280.00")
        BigDecimal refundAmount,
        @Schema(description = "Refund status.", example = "SUCCESS")
        String refundStatus,
        @Schema(description = "Order status after refund flow.", example = "REFUNDED")
        String orderStatus) {
}
