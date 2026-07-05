package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Artist information.")
public record ArtistRespDTO(
        @Schema(description = "Artist id.", example = "1001")
        Long artistId,
        @Schema(description = "Artist name.", example = "Sample Artist")
        String name,
        @Schema(description = "Artist description.")
        String description) {
}
