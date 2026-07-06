package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Venue information.")
public record VenueRespDTO(
        @Schema(description = "Venue id.", example = "5001")
        Long venueId,
        @Schema(description = "Venue name.", example = "Mercedes-Benz Arena")
        String name,
        @Schema(description = "Country.", example = "中国")
        String country,
        @Schema(description = "Province.", example = "北京市")
        String province,
        @Schema(description = "City.", example = "北京市")
        String city,
        @Schema(description = "District or county.", example = "朝阳区")
        String district,
        @Schema(description = "Town.", example = "望京街道")
        String town,
        @Schema(description = "Village.")
        String village,
        @Schema(description = "Street.", example = "阜通东大街")
        String street,
        @Schema(description = "House number.", example = "6号")
        String houseNumber,
        @Schema(description = "Estate or residential village.")
        String estate,
        @Schema(description = "Building.")
        String building,
        @Schema(description = "Formatted venue address.", example = "北京市朝阳区阜通东大街6号")
        String address) {
}
