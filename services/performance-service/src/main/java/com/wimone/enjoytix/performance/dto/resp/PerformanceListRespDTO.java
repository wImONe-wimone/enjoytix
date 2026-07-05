package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Performance list item response.")
public record PerformanceListRespDTO(
        @Schema(description = "Performance id.", example = "1001")
        Long performanceId,
        @Schema(description = "Performance title.", example = "Summer Live 2026")
        String title,
        @Schema(description = "Performance type.", example = "CONCERT")
        String performanceType,
        @Schema(description = "City.", example = "Shanghai")
        String city,
        @Schema(description = "Artist information.")
        ArtistRespDTO artist,
        @Schema(description = "Venue information.")
        VenueRespDTO venue,
        @Schema(description = "Earliest show time.", example = "2026-08-01T19:30:00")
        LocalDateTime earliestShowTime,
        @Schema(description = "Poster URL.")
        String posterUrl,
        @Schema(description = "Performance status.", example = "1")
        Integer status) {
}
