package com.wimone.enjoytix.ticket.dto.req;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TicketLockReqDTO {

    @NotNull
    private Long showId;

    @NotNull
    private Long categoryId;

    private Integer quantity = 1;

    private List<Long> seatIds = new ArrayList<>();

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
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds == null ? new ArrayList<>() : seatIds;
    }
}
