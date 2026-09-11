package com.wimone.enjoytix.performance.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seat information.")
public record SeatRespDTO(
        @Schema(description = "Seat id.", example = "400101")
        Long seatId,
        @Schema(description = "Seat area id.", example = "40001")
        Long areaId,
        @Schema(description = "Row number.", example = "1")
        Integer rowNo,
        @Schema(description = "Column number.", example = "8")
        Integer columnNo,
        @Schema(description = "Seat number.", example = "A1-08")
        String seatNo,
        @Schema(description = "Seat status.", example = "1")
        Integer status) {
}
