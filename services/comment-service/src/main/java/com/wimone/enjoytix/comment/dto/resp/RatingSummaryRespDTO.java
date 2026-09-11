package com.wimone.enjoytix.comment.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Project rating summary response.")
public record RatingSummaryRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Performance id.", type = "string")
        Long performanceId,
        @Schema(description = "Visible review count.")
        Integer reviewCount,
        @Schema(description = "Average rating.")
        BigDecimal avgRating,
        @Schema(description = "1-star review count.")
        Integer star1Count,
        @Schema(description = "2-star review count.")
        Integer star2Count,
        @Schema(description = "3-star review count.")
        Integer star3Count,
        @Schema(description = "4-star review count.")
        Integer star4Count,
        @Schema(description = "5-star review count.")
        Integer star5Count) {
}
