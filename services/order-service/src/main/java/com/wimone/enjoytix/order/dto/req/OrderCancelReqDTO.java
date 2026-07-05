package com.wimone.enjoytix.order.dto.req;

import jakarta.validation.constraints.NotNull;

public class OrderCancelReqDTO {

    @NotNull
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
