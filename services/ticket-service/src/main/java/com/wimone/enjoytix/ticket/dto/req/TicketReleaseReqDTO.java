package com.wimone.enjoytix.ticket.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ticket lock release request.")
public class TicketReleaseReqDTO {

    @NotNull
    @Schema(description = "Ticket lock id.", example = "5001")
    private Long lockId;

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }
}
