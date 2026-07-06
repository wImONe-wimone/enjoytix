package com.wimone.enjoytix.pay.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Refund by business order request.")
public class RefundByOrderReqDTO {

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Business order id.", type = "string", example = "6001")
    private Long orderId;

    @Schema(description = "Refund reason.", example = "Customer ticket refund")
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
