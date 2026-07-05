package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.constraints.NotNull;

public class TicketReleaseReqDTO {

    @NotNull
    private Long lockId;

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }
}
