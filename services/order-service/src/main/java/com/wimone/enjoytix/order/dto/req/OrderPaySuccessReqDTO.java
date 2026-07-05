package com.wimone.enjoytix.order.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Order payment success notification request.")
public class OrderPaySuccessReqDTO {

    @NotNull
    @Schema(description = "Order id.", example = "6001")
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
