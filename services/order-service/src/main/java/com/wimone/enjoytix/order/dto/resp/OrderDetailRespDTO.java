package com.wimone.enjoytix.order.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Order detail response.")
public record OrderDetailRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Order id.", type = "string", example = "332110931670601700")
        Long orderId,
        @Schema(description = "Order serial number.", example = "EO6001")
        String orderSn,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Owner user id.", type = "string", example = "1")
        Long userId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Show session id.", type = "string", example = "2001")
        Long showId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Ticket lock id.", type = "string", example = "5001")
        Long lockId,
        @Schema(description = "Total order amount.", example = "1280.00")
        BigDecimal totalAmount,
        @Schema(description = "Order status.", example = "PENDING_PAYMENT")
        String status,
        @Schema(description = "Payment expiration time.", example = "2026-08-01T19:45:00")
        LocalDateTime payExpireTime,
        @Schema(description = "Order item list.")
        List<OrderItemRespDTO> items) {
}
