package com.wimone.enjoytix.order.dto.req;

import com.wimone.enjoytix.framework.base.ticket.TicketAllocationModeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AgentOrderCreateReqDTO {
    @NotBlank
    private String draftId;
    @NotBlank
    private String confirmationToken;
    @NotNull private Long showId;
    @NotNull private Long categoryId;
    @NotNull private Integer quantity;
    @NotNull private List<Long> seatIds;

    public OrderCreateReqDTO toOrderCreateRequest() {
        OrderCreateReqDTO request = new OrderCreateReqDTO();
        request.setShowId(showId);
        request.setCategoryId(categoryId);
        request.setQuantity(quantity);
        request.setSeatIds(seatIds);
        return request;
    }

    public String getDraftId() { return draftId; }
    public void setDraftId(String draftId) { this.draftId = draftId; }
    public String getConfirmationToken() { return confirmationToken; }
    public void setConfirmationToken(String confirmationToken) { this.confirmationToken = confirmationToken; }
    public Long getShowId() { return showId; }
    public void setShowId(Long showId) { this.showId = showId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds == null ? new ArrayList<>() : seatIds; }
}
