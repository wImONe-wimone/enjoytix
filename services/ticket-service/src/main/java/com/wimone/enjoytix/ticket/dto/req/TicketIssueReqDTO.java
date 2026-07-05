package com.wimone.enjoytix.ticket.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ticket issue request.")
public class TicketIssueReqDTO {

    @NotNull
    @Schema(description = "Ticket lock id.", example = "5001")
    private Long lockId;

    @NotNull
    @Schema(description = "Order id.", example = "6001")
    private Long orderId;

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
