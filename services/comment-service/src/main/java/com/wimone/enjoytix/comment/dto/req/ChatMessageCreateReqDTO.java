package com.wimone.enjoytix.comment.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Chat message creation request.")
public record ChatMessageCreateReqDTO(
        @NotBlank
        @Size(max = 1000)
        @Schema(description = "Chat message content.", example = "Looking forward to this show.")
        String content) {
}
