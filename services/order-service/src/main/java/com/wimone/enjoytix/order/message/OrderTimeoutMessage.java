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
        int roundedMillis = (expireTime.getNano() + 500_000) / 1_000_000;
        if (roundedMillis >= 1000) {
            return expireTime.plusSeconds(1).withNano(0);
        }
        return expireTime.withNano(roundedMillis * 1_000_000);
    }
}
