package com.wimone.enjoytix.performance.message;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class LocalPerformanceSaleStartMessageListener {

    private final PerformanceSaleStartMessageProcessor processor;

    public LocalPerformanceSaleStartMessageListener(PerformanceSaleStartMessageProcessor processor) {
        this.processor = processor;
    }

    @EventListener
    public void onMessage(PerformanceSaleStartMessage message) {
        processor.consume(message, 0, false);
    }
}
