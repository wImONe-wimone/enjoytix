package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Venue information.")
public record VenueRespDTO(
        @Schema(description = "Venue id.", example = "5001")
        Long venueId,
        @Schema(description = "Venue name.", example = "Mercedes-Benz Arena")
        String name,
        @Schema(description = "Venue city.", example = "Shanghai")
        String city,
        @Schema(description = "Venue address.")
        String address) {
}
