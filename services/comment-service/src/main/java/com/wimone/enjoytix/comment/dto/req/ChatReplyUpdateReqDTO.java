package com.wimone.enjoytix.comment.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Chat reply update request.")
public record ChatReplyUpdateReqDTO(
        @NotBlank
        @Size(max = 1000)
        @Schema(description = "Updated reply content.", example = "Updated reply.")
        String content) {
}
