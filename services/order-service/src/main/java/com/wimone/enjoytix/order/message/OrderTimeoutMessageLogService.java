package com.wimone.enjoytix.order.message;

import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.order.common.enums.OrderTimeoutMessageStatusEnum;
import com.wimone.enjoytix.order.config.OrderTimeoutMessageProperties;
import com.wimone.enjoytix.order.dao.entity.OrderTimeoutMessageLogDO;
import com.wimone.enjoytix.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderTimeoutMessageLogService {

    private static final int MAX_ERROR_LENGTH = 512;

    private final OrderRepository orderRepository;
    private final IdGeneratorManager idGeneratorManager;
    private final OrderTimeoutMessageProperties properties;

    public OrderTimeoutMessageLogService(
            OrderRepository orderRepository,
            IdGeneratorManager idGeneratorManager,
            OrderTimeoutMessageProperties properties) {
        this.orderRepository = orderRepository;
        this.idGeneratorManager = idGeneratorManager;
        this.properties = properties;
    }

    public void recordSent(OrderTimeoutMessage message) {
        OrderTimeoutMessageLogDO logDO = findOrCreate(message);
        if (isSuccess(logDO)) {
            return;
        }
        logDO.setStatus(OrderTimeoutMessageStatusEnum.SENT.name());
        logDO.setRetryCount(0);
        logDO.setLastError(null);
        touch(logDO);
        orderRepository.saveTimeoutMessageLog(logDO);
    }

    public void recordSendFailed(OrderTimeoutMessage message, Throwable throwable) {
        OrderTimeoutMessageLogDO logDO = findOrCreate(message);
        if (isSuccess(logDO)) {
            return;
        }
        logDO.setStatus(OrderTimeoutMessageStatusEnum.SEND_FAILED.name());
        logDO.setRetryCount(0);
        logDO.setLastError(errorMessage(throwable));
        touch(logDO);
        orderRepository.saveTimeoutMessageLog(logDO);
    }

    public OrderTimeoutMessageLogDO findOrCreate(OrderTimeoutMessage message) {
        return orderRepository.findTimeoutMessageLog(message.messageKey())
                .orElseGet(() -> {
                    OrderTimeoutMessageLogDO logDO = new OrderTimeoutMessageLogDO();
                    logDO.setId(idGeneratorManager.nextId());
                    logDO.setMessageKey(message.messageKey());
                    logDO.setOrderId(message.orderId());
                    logDO.setLockId(message.lockId());
                    logDO.setExpireTime(message.expireTime());
                    logDO.setStatus(OrderTimeoutMessageStatusEnum.SENT.name());
                    logDO.setRetryCount(0);
                    logDO.setCreateTime(LocalDateTime.now());
                    logDO.setUpdateTime(LocalDateTime.now());
                    logDO.setDelFlag(0);
                    orderRepository.saveTimeoutMessageLogIfAbsent(logDO);
                    return orderRepository.findTimeoutMessageLog(message.messageKey()).orElse(logDO);
                });
    }

    public boolean isSuccess(OrderTimeoutMessageLogDO logDO) {
        return OrderTimeoutMessageStatusEnum.SUCCESS.name().equals(logDO.getStatus());
    }

    public void markConsuming(OrderTimeoutMessageLogDO logDO, int reconsumeTimes) {
        logDO.setStatus(OrderTimeoutMessageStatusEnum.CONSUMING.name());
        logDO.setRetryCount(Math.max(retryCount(logDO), reconsumeTimes));
        logDO.setLastError(null);
        touch(logDO);
        orderRepository.saveTimeoutMessageLog(logDO);
    }

    public void markSuccess(OrderTimeoutMessageLogDO logDO) {
        logDO.setStatus(OrderTimeoutMessageStatusEnum.SUCCESS.name());
        logDO.setLastError(null);
        logDO.setConsumeTime(LocalDateTime.now());
        touch(logDO);
        orderRepository.saveTimeoutMessageLog(logDO);
    }

    public void markFailure(OrderTimeoutMessageLogDO logDO, int reconsumeTimes, RuntimeException ex) {
        int currentRetryCount = Math.max(retryCount(logDO), reconsumeTimes + 1);
        logDO.setRetryCount(currentRetryCount);
        logDO.setStatus(currentRetryCount >= properties.getMaxReconsumeTimes()
                ? OrderTimeoutMessageStatusEnum.DEAD_LETTER.name()
                : OrderTimeoutMessageStatusEnum.RETRYING.name());
        logDO.setLastError(errorMessage(ex));
        touch(logDO);
        orderRepository.saveTimeoutMessageLog(logDO);
    }

    public List<OrderTimeoutMessageLogDO> listForCompensation(LocalDateTime now) {
        return orderRepository.listTimeoutMessageLogsForCompensation(now, properties.getCompensationBatchSize());
    }

    private void touch(OrderTimeoutMessageLogDO logDO) {
        logDO.setUpdateTime(LocalDateTime.now());
        if (logDO.getCreateTime() == null) {
            logDO.setCreateTime(logDO.getUpdateTime());
        }
        if (logDO.getDelFlag() == null) {
            logDO.setDelFlag(0);
        }
    }

    private int retryCount(OrderTimeoutMessageLogDO logDO) {
        return logDO.getRetryCount() == null ? 0 : logDO.getRetryCount();
    }

    private String errorMessage(Throwable throwable) {
        String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
