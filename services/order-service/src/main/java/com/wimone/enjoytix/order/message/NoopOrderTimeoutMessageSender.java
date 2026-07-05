package com.wimone.enjoytix.order.message;

public class NoopOrderTimeoutMessageSender implements OrderTimeoutMessageSender {

    @Override
    public void send(OrderTimeoutMessage message) {
    }
}
