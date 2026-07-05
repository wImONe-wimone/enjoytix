package com.wimone.enjoytix.ticket.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Ticket lock request.")
public class TicketLockReqDTO {

    @NotNull
    @Schema(description = "Show session id.", example = "2001")
    private Long showId;

    @NotNull
    @Schema(description = "Ticket category id.", example = "3001")
    private Long categoryId;

    @Schema(description = "Ticket quantity when no explicit seats are selected.", example = "1")
    private Integer quantity = 1;

    @Schema(description = "Selected seat ids. Leave empty for non-seat-selectable categories.", example = "[400101]")
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
