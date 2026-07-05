package com.wimone.enjoytix.order.message;

import java.time.LocalDateTime;

public record OrderTimeoutMessage(
        Long orderId,
        Long lockId,
        LocalDateTime expireTime
) {
}
