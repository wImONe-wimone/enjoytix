package com.wimone.enjoytix.ticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

@TableName("t_seat_stock")
public class SeatStockDO extends BaseDO {

    private Long showId;
    private Long categoryId;
    private Long seatId;
    private String areaName;
    private Integer rowNo;
    private Integer columnNo;
    private String seatNo;
    private String status;
    private Long lockId;

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

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getColumnNo() {
        return columnNo;
    }

    public void setColumnNo(Integer columnNo) {
        this.columnNo = columnNo;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }
}
