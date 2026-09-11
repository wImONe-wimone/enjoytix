package com.wimone.enjoytix.comment.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Chat reply response.")
public record ChatReplyRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Reply id.", type = "string")
        Long replyId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Parent message id.", type = "string")
        Long messageId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Performance id.", type = "string")
        Long performanceId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Author user id.", type = "string")
        Long userId,
        @Schema(description = "Reply content.")
        String content,
        @Schema(description = "Edit count.")
        Integer editCount,
        @Schema(description = "Creation time.")
        LocalDateTime createTime,
        @Schema(description = "Last update time.")
        LocalDateTime updateTime) {
}
