package com.wimone.enjoytix.order.service;

import java.time.LocalDateTime;

public interface OrderTimeoutCloseService {

    boolean closeIfExpired(Long orderId, LocalDateTime expectedExpireTime, String reason);
}
