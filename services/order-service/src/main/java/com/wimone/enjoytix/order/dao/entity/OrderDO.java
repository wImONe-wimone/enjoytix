package com.wimone.enjoytix.order.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("t_order")
public class OrderDO extends BaseDO {

    private String orderSn;
    private Long userId;
    private Long showId;
    private Long lockId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime payExpireTime;

    public String getOrderSn() {
        return orderSn;
    }

    public void setOrderSn(String orderSn) {
        this.orderSn = orderSn;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPayExpireTime() {
        return payExpireTime;
    }

    public void setPayExpireTime(LocalDateTime payExpireTime) {
        this.payExpireTime = payExpireTime;
    }
}
