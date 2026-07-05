package com.wimone.enjoytix.order.message;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalOrderTimeoutMessageListener {

    private final OrderTimeoutMessageProcessor processor;

    public LocalOrderTimeoutMessageListener(OrderTimeoutMessageProcessor processor) {
        this.processor = processor;
    }

    @EventListener
    public void onMessage(OrderTimeoutMessage message) {
        processor.consume(message, 0, false);
    }
}
