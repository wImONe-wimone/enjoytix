package com.wimone.enjoytix.order.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.time.LocalDateTime;

@TableName("et_order_timeout_message_log")
public class OrderTimeoutMessageLogDO extends BaseDO {

    private String messageKey;
    private Long orderId;
    private Long lockId;
    private LocalDateTime expireTime;
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime consumeTime;

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getConsumeTime() {
        return consumeTime;
    }

    public void setConsumeTime(LocalDateTime consumeTime) {
        this.consumeTime = consumeTime;
    }
}
