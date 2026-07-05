package com.wimone.enjoytix.pay.dto.req;

import jakarta.validation.constraints.NotNull;

public class RefundApplyReqDTO {

    @NotNull
    private Long payId;

    private String reason;

    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
