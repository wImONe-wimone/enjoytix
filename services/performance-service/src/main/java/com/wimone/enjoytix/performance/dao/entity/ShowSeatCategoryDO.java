package com.wimone.enjoytix.performance.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

@TableName("et_show_seat_category")
public class ShowSeatCategoryDO extends BaseDO {

    private Long showId;
    private Long categoryId;
    private Long seatId;
    private Integer saleLocked;

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

    public Integer getSaleLocked() {
        return saleLocked;
    }

    public void setSaleLocked(Integer saleLocked) {
        this.saleLocked = saleLocked;
    }
}
