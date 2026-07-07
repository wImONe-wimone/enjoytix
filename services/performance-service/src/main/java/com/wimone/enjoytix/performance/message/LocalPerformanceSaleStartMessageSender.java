package com.wimone.enjoytix.performance.message;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@Profile("test")
public class LocalPerformanceSaleStartMessageSender implements PerformanceSaleStartMessageSender {

    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    public LocalPerformanceSaleStartMessageSender(TaskScheduler taskScheduler, ApplicationEventPublisher eventPublisher) {
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void send(PerformanceSaleStartMessage message) {
        taskScheduler.schedule(
                () -> eventPublisher.publishEvent(message),
                message.scheduledSaleTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
