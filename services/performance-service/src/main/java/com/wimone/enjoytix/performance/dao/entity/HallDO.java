package com.wimone.enjoytix.performance.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimone.enjoytix.framework.database.base.BaseDO;

@TableName("et_hall")
public class HallDO extends BaseDO {

    private Long venueId;
    private String name;
    private Long seatMapId;

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSeatMapId() {
        return seatMapId;
    }

    public void setSeatMapId(Long seatMapId) {
        this.seatMapId = seatMapId;
    }
}
