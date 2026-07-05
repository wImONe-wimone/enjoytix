package com.wimone.enjoytix.order.dto.req;

import jakarta.validation.constraints.NotNull;

public class OrderPaySuccessReqDTO {

    @NotNull
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
