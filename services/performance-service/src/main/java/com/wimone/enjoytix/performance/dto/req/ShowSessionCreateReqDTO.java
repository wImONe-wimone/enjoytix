package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Show session creation request.")
public class ShowSessionCreateReqDTO {

    @NotNull
    @Schema(description = "Show start time.", example = "2026-08-16T19:30:00")
    private LocalDateTime showTime;

    @NotNull
    @Min(1)
    @Max(1440)
    @Schema(description = "Show duration in minutes.", example = "120")
    private Integer durationMinutes;

    @Min(0)
    @Max(1)
    @Schema(description = "Show status, 1 means enabled and 0 means disabled.", example = "1")
    private Integer status = 1;

    @Valid
    @Schema(description = "Ticket categories and seat mappings for this show. If empty, the service may copy from an existing show under the same performance.")
    private List<TicketCategoryConfigReqDTO> ticketCategories;

    public LocalDateTime getShowTime() {
        return showTime;
    }

    public void setShowTime(LocalDateTime showTime) {
        this.showTime = showTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<TicketCategoryConfigReqDTO> getTicketCategories() {
        return ticketCategories;
    }

    public void setTicketCategories(List<TicketCategoryConfigReqDTO> ticketCategories) {
        this.ticketCategories = ticketCategories;
    }
}
