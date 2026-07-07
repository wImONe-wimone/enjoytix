package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Show ticket category and optional seat mapping configuration.")
public class TicketCategoryConfigReqDTO {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Ticket category name.", example = "VIP")
    private String categoryName;

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "Ticket price.", example = "1280.00")
    private BigDecimal price;

    @NotNull
    @Min(1)
    @Schema(description = "Total stock for this category.", example = "12")
    private Integer totalStock;

    @Min(0)
    @Max(1)
    @Schema(description = "Whether seats are selectable, 1 means selectable.", example = "1")
    private Integer seatSelectable = 1;

    @Schema(description = "Deprecated compatibility field. Seat sale availability is controlled by assigned seats and lockedSeatIds.", example = "1")
    private Integer status = 1;

    @Schema(description = "Seat map id used by the client when assigning seats.")
    private Long seatMapId;

    @Schema(description = "Seat ids assigned to this category when seats are selectable.")
    private List<Long> seatIds;

    @Schema(description = "Seat ids that should be locked before sale and initialized as not sellable.")
    private List<Long> lockedSeatIds;

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

    public Long getSeatMapId() {
        return seatMapId;
    }

    public void setSeatMapId(Long seatMapId) {
        this.seatMapId = seatMapId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public List<Long> getLockedSeatIds() {
        return lockedSeatIds;
    }

    public void setLockedSeatIds(List<Long> lockedSeatIds) {
        this.lockedSeatIds = lockedSeatIds;
    }
}
