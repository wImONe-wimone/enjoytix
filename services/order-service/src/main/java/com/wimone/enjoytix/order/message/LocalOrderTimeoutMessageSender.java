package com.wimone.enjoytix.order.message;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class LocalOrderTimeoutMessageSender implements OrderTimeoutMessageSender {

    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    public LocalOrderTimeoutMessageSender(TaskScheduler taskScheduler, ApplicationEventPublisher eventPublisher) {
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void send(OrderTimeoutMessage message) {
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(message),
                message.expireTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
