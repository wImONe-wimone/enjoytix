package com.wimone.enjoytix.pay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Refund application request.")
public class RefundApplyReqDTO {

    @NotNull
    @Schema(description = "Pay order id.", example = "7001")
    private Long payId;

    @Schema(description = "Refund reason.", example = "Customer request")
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
