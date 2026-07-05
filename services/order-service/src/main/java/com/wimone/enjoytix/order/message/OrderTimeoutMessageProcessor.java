package com.wimone.enjoytix.order.message;

import com.wimone.enjoytix.order.dao.entity.OrderTimeoutMessageLogDO;
import com.wimone.enjoytix.order.service.OrderTimeoutCloseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderTimeoutMessageProcessor {

    private final OrderTimeoutMessageLogService logService;
    private final OrderTimeoutCloseService orderTimeoutCloseService;

    public OrderTimeoutMessageProcessor(
            OrderTimeoutMessageLogService logService,
            OrderTimeoutCloseService orderTimeoutCloseService) {
        this.logService = logService;
        this.orderTimeoutCloseService = orderTimeoutCloseService;
    }

    public void consume(OrderTimeoutMessage message, int reconsumeTimes, boolean throwOnFailure) {
        OrderTimeoutMessageLogDO logDO = logService.findOrCreate(message);
        if (logService.isSuccess(logDO)) {
            return;
        }
        try {
            if (message.expireTime().isAfter(LocalDateTime.now())) {
                throw new IllegalStateException("Order timeout message delivered before expire time");
            }
            logService.markConsuming(logDO, reconsumeTimes);

            boolean completed = orderTimeoutCloseService.closeIfExpired(
                    message.orderId(),
                    message.expireTime(),
                    "payment timeout message"
            );
            if (!completed) {
                throw new IllegalStateException("Order timeout close skipped");
            }
            logService.markSuccess(logDO);
        } catch (RuntimeException ex) {
            logService.markFailure(logDO, reconsumeTimes, ex);
            if (throwOnFailure) {
                throw ex;
            }
        }
    }

    public int compensateExpiredMessages() {
        List<OrderTimeoutMessageLogDO> logs = logService.listForCompensation(LocalDateTime.now());
        logs.forEach(each -> consume(
                new OrderTimeoutMessage(each.getOrderId(), each.getLockId(), each.getExpireTime()),
                each.getRetryCount(),
                false
        ));
        return logs.size();
    }
}
