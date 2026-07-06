package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "Performance page query request.")
public class PerformancePageQueryReqDTO {

    @Schema(description = "Performance type.", example = "CONCERT")
    private String performanceType;
    @Schema(description = "Artist name keyword.", example = "Taylor")
    private String artistName;
    @Schema(description = "Venue name keyword.", example = "Mercedes-Benz Arena")
    private String venueName;

    @Schema(description = "Show date.", example = "2026-08-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate showDate;

    @Min(1)
    @Schema(description = "Current page number.", example = "1")
    private long current = 1;

    @Min(1)
    @Max(200)
    @Schema(description = "Page size, maximum 200.", example = "20")
    private long size = 20;

    public String getPerformanceType() {
        return performanceType;
    }

    public void setPerformanceType(String performanceType) {
        this.performanceType = performanceType;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current <= 0 ? 1 : current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size <= 0 ? 20 : Math.min(size, 200);
    }
}
