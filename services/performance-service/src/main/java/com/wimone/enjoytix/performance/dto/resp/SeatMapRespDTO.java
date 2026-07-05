package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Seat map response.")
public record SeatMapRespDTO(
        @Schema(description = "Seat map id.", example = "3001")
        Long seatMapId,
        @Schema(description = "Seat map name.", example = "Main Hall Seat Map")
        String name,
        @Schema(description = "Seat row count.", example = "30")
        Integer rowCount,
        @Schema(description = "Seat column count.", example = "40")
        Integer columnCount,
        @Schema(description = "Seat list.")
        List<SeatRespDTO> seats) {
}
