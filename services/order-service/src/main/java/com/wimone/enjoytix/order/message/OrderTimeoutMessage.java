package com.wimone.enjoytix.order.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public record OrderTimeoutMessage(
        Long orderId,
        Long lockId,
        LocalDateTime expireTime
) {

    public OrderTimeoutMessage {
        expireTime = normalizeExpireTime(expireTime);
    }

    @JsonIgnore
    public String messageKey() {
        return "order-timeout:" + orderId + ":" + expireTime;
    }

    public static LocalDateTime normalizeExpireTime(LocalDateTime expireTime) {
        if (expireTime == null) {
            return null;
        }
        return expireTime.withNano((expireTime.getNano() / 1_000_000) * 1_000_000);
    }
}
