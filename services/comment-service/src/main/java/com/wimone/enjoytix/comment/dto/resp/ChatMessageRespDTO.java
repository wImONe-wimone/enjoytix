package com.wimone.enjoytix.comment.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Chat message response.")
public record ChatMessageRespDTO(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Message id.", type = "string")
        Long messageId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Performance id.", type = "string")
        Long performanceId,
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "Author user id.", type = "string")
        Long userId,
        @Schema(description = "Message content.")
        String content,
        @Schema(description = "Edit count.")
        Integer editCount,
        @Schema(description = "Active reply count.")
        Long replyCount,
        @Schema(description = "First page replies.")
        CursorPageRespDTO<ChatReplyRespDTO> replies,
        @Schema(description = "Creation time.")
        LocalDateTime createTime,
        @Schema(description = "Last update time.")
        LocalDateTime updateTime) {
}
