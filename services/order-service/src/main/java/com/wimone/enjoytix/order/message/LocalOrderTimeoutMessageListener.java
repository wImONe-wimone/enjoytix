package com.wimone.enjoytix.order.message;

import com.wimone.enjoytix.order.service.OrderTimeoutCloseService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LocalOrderTimeoutMessageListener {

    private final OrderTimeoutCloseService orderTimeoutCloseService;

    public LocalOrderTimeoutMessageListener(OrderTimeoutCloseService orderTimeoutCloseService) {
        this.orderTimeoutCloseService = orderTimeoutCloseService;
    }

    @EventListener
    public void onMessage(OrderTimeoutMessage message) {
        orderTimeoutCloseService.closeIfExpired(message.orderId(), message.expireTime(), "payment timeout message");
    }
}
