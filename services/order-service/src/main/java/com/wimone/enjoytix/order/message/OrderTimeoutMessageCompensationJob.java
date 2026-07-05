package com.wimone.enjoytix.order.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class OrderTimeoutMessageCompensationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutMessageCompensationJob.class);

    private final OrderTimeoutMessageProcessor processor;

    public OrderTimeoutMessageCompensationJob(OrderTimeoutMessageProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${order.timeout-message.compensation-fixed-delay-millis:60000}")
    public void compensate() {
        int count = processor.compensateExpiredMessages();
        if (count > 0) {
            log.info("Compensated order timeout messages, count={}", count);
        }
    }
}
