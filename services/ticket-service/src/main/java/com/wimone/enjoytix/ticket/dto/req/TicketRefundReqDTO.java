package com.wimone.enjoytix.ticket.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Issued ticket refund request.")
public class TicketRefundReqDTO {

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Ticket lock id.", type = "string", example = "5001")
    private Long lockId;

    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "Business order id.", type = "string", example = "6001")
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
