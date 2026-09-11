package com.wimone.enjoytix.comment.remote.dto;

import java.time.LocalDateTime;

public record OrderPurchaseCheckRespDTO(
        Boolean purchased,
        Long orderId,
        LocalDateTime latestPaidTime) {
}
