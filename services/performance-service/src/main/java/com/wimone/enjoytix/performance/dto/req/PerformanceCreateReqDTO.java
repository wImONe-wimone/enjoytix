package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "Performance creation request.")
public class PerformanceCreateReqDTO {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Performance title.", example = "Aurora Band 2026 Live")
    private String title;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Performance type.", example = "CONCERT")
    private String performanceType;

    @NotNull
    @Schema(description = "Artist id.", example = "100")
    private Long artistId;

    @NotNull
    @Schema(description = "Venue id.", example = "200")
    private Long venueId;

    @Size(max = 512)
    @Schema(description = "Poster URL.")
    private String posterUrl;

    @Size(max = 1024)
    @Schema(description = "Performance description.")
    private String description;

    @Min(0)
    @Max(1)
    @Schema(description = "Performance status, 1 means enabled and 0 means disabled.", example = "1")
    private Integer status = 1;

    @Valid
    @Schema(description = "Optional initial show sessions with ticket category and seat mapping configuration.")
    private List<ShowSessionCreateReqDTO> showSessions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPerformanceType() {
        return performanceType;
    }

    public void setPerformanceType(String performanceType) {
        this.performanceType = performanceType;
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<ShowSessionCreateReqDTO> getShowSessions() {
        return showSessions;
    }

    public void setShowSessions(List<ShowSessionCreateReqDTO> showSessions) {
        this.showSessions = showSessions;
    }
}
