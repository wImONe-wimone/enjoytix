package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.constraints.NotNull;

public class TicketIssueReqDTO {

    @NotNull
    private Long lockId;

    @NotNull
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
