package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Show session response.")
public record ShowSessionRespDTO(
        @Schema(description = "Show session id.", example = "2001")
        Long showId,
        @Schema(description = "Performance id.", example = "1001")
        Long performanceId,
        @Schema(description = "Hall id.", example = "3001")
        Long hallId,
        @Schema(description = "Show time.", example = "2026-08-01T19:30:00")
        LocalDateTime showTime,
        @Schema(description = "Sale start time.", example = "2026-07-01T10:00:00")
        LocalDateTime saleStartTime,
        @Schema(description = "Sale end time.", example = "2026-08-01T19:00:00")
        LocalDateTime saleEndTime,
        @Schema(description = "Show status.", example = "1")
        Integer status) {
}
