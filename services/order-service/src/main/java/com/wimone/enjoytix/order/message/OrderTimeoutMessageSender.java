package com.wimone.enjoytix.order.message;

public interface OrderTimeoutMessageSender {

    void send(OrderTimeoutMessage message);
}
