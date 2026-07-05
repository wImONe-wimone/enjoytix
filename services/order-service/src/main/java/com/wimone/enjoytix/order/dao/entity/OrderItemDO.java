package com.wimone.enjoytix.order.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@TableName(value = "et_order_item", autoResultMap = true)
public class OrderItemDO extends BaseDO {

    private Long orderId;
    private Long showId;
    private Long categoryId;
    private Integer quantity;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> seatIds;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> ticketCodes;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public List<Long> getSeatIds() {
        seatIds = normalizeLongList(seatIds);
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = normalizeLongList(seatIds);
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<String> getTicketCodes() {
        return ticketCodes;
    }

    public void setTicketCodes(List<String> ticketCodes) {
        this.ticketCodes = ticketCodes;
    }

    private List<Long> normalizeLongList(List<?> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        List<Long> result = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof Number number) {
                result.add(number.longValue());
            } else if (value != null) {
                result.add(Long.valueOf(value.toString()));
            }
        }
        return result;
    }
}
