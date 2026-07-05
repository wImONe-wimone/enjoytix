package com.wimone.enjoytix.pay.dto.req;

import jakarta.validation.constraints.NotNull;

public class PayCreateReqDTO {

    @NotNull
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
