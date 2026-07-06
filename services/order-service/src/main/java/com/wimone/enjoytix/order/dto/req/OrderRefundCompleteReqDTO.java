package com.wimone.enjoytix.order.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Order refund completion request.")
public class OrderRefundCompleteReqDTO {

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Order id.", type = "string", example = "6001")
    private Long orderId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Refund order id.", type = "string", example = "8001")
    private Long refundId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }
}
