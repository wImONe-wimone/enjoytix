package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Performance scheduled sale request.")
public class PerformanceSaleScheduleReqDTO {

    @NotNull
    @Schema(description = "Scheduled sale start time.", example = "2026-08-01T10:00:00")
    private LocalDateTime saleStartTime;

    public LocalDateTime getSaleStartTime() {
        return saleStartTime;
    }

    public void setSaleStartTime(LocalDateTime saleStartTime) {
        this.saleStartTime = saleStartTime;
    }
}
