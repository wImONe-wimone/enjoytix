package com.wimone.enjoytix.comment.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Project review creation request.")
public record ReviewCreateReqDTO(
        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Rating from 1 to 5.", example = "5")
        Integer rating,
        @NotBlank
        @Size(max = 2000)
        @Schema(description = "Review content.", example = "Great performance and smooth ticketing.")
        String content) {
}
