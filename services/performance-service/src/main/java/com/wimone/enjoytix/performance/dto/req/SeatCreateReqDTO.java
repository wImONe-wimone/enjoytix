package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Venue seat creation request.")
public class SeatCreateReqDTO {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Seat area name.", example = "A Zone")
    private String areaName;

    @NotNull
    @Min(1)
    @Max(1000)
    @Schema(description = "Row number.", example = "1")
    private Integer rowNo;

    @NotNull
    @Min(1)
    @Max(1000)
    @Schema(description = "Column number.", example = "8")
    private Integer columnNo;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Seat number.", example = "A1-08")
    private String seatNo;

    @Min(0)
    @Max(1)
    @Schema(description = "Seat status, 1 means enabled and 0 means disabled.", example = "1")
    private Integer status = 1;

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getColumnNo() {
        return columnNo;
    }

    public void setColumnNo(Integer columnNo) {
        this.columnNo = columnNo;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
