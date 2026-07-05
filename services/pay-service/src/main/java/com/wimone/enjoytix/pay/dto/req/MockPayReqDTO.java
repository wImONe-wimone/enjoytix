package com.wimone.enjoytix.pay.dto.req;

import jakarta.validation.constraints.NotNull;

public class MockPayReqDTO {

    @NotNull
    private Long payId;

    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
}
