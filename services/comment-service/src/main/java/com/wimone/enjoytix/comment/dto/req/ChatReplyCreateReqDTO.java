package com.wimone.enjoytix.comment.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Chat reply creation request.")
public record ChatReplyCreateReqDTO(
        @NotBlank
        @Size(max = 1000)
        @Schema(description = "Reply content.", example = "I agree with you.")
        String content) {
}
