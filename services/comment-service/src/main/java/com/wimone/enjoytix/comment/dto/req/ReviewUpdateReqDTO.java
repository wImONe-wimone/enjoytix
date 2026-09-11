package com.wimone.enjoytix.comment.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Project review update request.")
public record ReviewUpdateReqDTO(
        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Rating from 1 to 5.", example = "4")
        Integer rating,
        @NotBlank
        @Size(max = 2000)
        @Schema(description = "Updated review content.", example = "Good performance, but entry was crowded.")
        String content) {
}
