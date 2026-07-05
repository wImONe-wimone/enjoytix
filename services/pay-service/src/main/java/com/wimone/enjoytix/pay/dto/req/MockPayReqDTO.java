package com.wimone.enjoytix.pay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mock payment success request.")
public class MockPayReqDTO {

    @NotNull
    @Schema(description = "Pay order id.", example = "7001")
    private Long payId;

    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
}
