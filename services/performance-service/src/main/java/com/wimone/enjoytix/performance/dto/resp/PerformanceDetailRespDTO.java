package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Performance detail response.")
public record PerformanceDetailRespDTO(
        @Schema(description = "Performance id.", example = "1001")
        Long performanceId,
        @Schema(description = "Performance title.", example = "Summer Live 2026")
        String title,
        @Schema(description = "Performance type.", example = "CONCERT")
        String performanceType,
        @Schema(description = "City.", example = "Shanghai")
        String city,
        @Schema(description = "Poster URL.")
        String posterUrl,
        @Schema(description = "Performance description.")
        String description,
        @Schema(description = "Performance status.", example = "1")
        Integer status,
        @Schema(description = "Artist information.")
        ArtistRespDTO artist,
        @Schema(description = "Venue information.")
        VenueRespDTO venue,
        @Schema(description = "Show sessions.")
        List<ShowSessionRespDTO> sessions) {
}
