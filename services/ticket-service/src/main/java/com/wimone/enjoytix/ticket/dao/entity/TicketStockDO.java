package com.wimone.enjoytix.ticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.math.BigDecimal;

@TableName("et_ticket_stock")
public class TicketStockDO extends BaseDO {

    private Long showId;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer soldStock;
    private Integer seatSelectable;

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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getLockedStock() {
        return lockedStock;
    }

    public void setLockedStock(Integer lockedStock) {
        this.lockedStock = lockedStock;
    }

    public Integer getSoldStock() {
        return soldStock;
    }

    public void setSoldStock(Integer soldStock) {
        this.soldStock = soldStock;
    }

    public Integer getSeatSelectable() {
        return seatSelectable;
    }

    public void setSeatSelectable(Integer seatSelectable) {
        this.seatSelectable = seatSelectable;
    }

    public int availableStock() {
        return totalStock - lockedStock - soldStock;
    }
}
