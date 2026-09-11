package com.wimone.enjoytix.comment.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Project review response.")
public record ProjectReviewRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Review id.", type = "string")
        Long reviewId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Performance id.", type = "string")
        Long performanceId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Author user id.", type = "string")
        Long userId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Related paid order id.", type = "string")
        Long orderId,
        @Schema(description = "Rating from 1 to 5.")
        Integer rating,
        @Schema(description = "Review content.")
        String content,
        @Schema(description = "Edit count.")
        Integer editCount,
        @Schema(description = "Creation time.")
        LocalDateTime createTime,
        @Schema(description = "Last update time.")
        LocalDateTime updateTime) {
}
