package com.wimone.enjoytix.performance.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.math.BigDecimal;

@TableName("et_ticket_category")
public class TicketCategoryDO extends BaseDO {

    private Long showId;
    private String categoryName;
    private BigDecimal price;
    private Integer totalStock;
    private Integer remainingStock;
    private Integer seatSelectable;
    private Integer status;

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
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

    public Integer getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(Integer remainingStock) {
        this.remainingStock = remainingStock;
    }

    public Integer getSeatSelectable() {
        return seatSelectable;
    }

    public void setSeatSelectable(Integer seatSelectable) {
        this.seatSelectable = seatSelectable;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
