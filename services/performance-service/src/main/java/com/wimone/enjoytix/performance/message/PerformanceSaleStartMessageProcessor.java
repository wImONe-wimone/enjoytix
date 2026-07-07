package com.wimone.enjoytix.performance.message;

import com.wimone.enjoytix.performance.service.PerformanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PerformanceSaleStartMessageProcessor {

    private final PerformanceService performanceService;
    private final PerformanceSaleStartMessageSender messageSender;

    public PerformanceSaleStartMessageProcessor(
            PerformanceService performanceService,
            PerformanceSaleStartMessageSender messageSender) {
        this.performanceService = performanceService;
        this.messageSender = messageSender;
    }

    public void consume(PerformanceSaleStartMessage message, int reconsumeTimes, boolean throwOnFailure) {
        try {
            if (message.scheduledSaleTime().isAfter(LocalDateTime.now())) {
                messageSender.send(message);
                return;
            }
            performanceService.startScheduledSale(message.operatorId(), message.performanceId(), message.scheduledSaleTime());
        } catch (RuntimeException ex) {
            if (throwOnFailure) {
                throw ex;
            }
        }
    }
}
