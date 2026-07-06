package com.wimone.enjoytix.order.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Order refund rollback request.")
public class OrderRefundRollbackReqDTO {

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Order id.", type = "string", example = "6001")
    private Long orderId;

    @Schema(description = "Rollback reason.", example = "Refund failed")
    private String reason;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
