package com.wimone.enjoytix.order.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Order purchase check response.")
public record OrderPurchaseCheckRespDTO(
        @Schema(description = "Whether the user has a paid order for the performance.")
        Boolean purchased,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Latest paid order id.", type = "string")
        Long orderId,
        @Schema(description = "Latest paid time inferred from order update time.")
        LocalDateTime latestPaidTime) {
}
