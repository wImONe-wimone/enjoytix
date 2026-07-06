package com.wimone.enjoytix.order.remote.dto;

import java.io.Serializable;

public record TicketRefundReqDTO(Long lockId, Long orderId) implements Serializable {
}
