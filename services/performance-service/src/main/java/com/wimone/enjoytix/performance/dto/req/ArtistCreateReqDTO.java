package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Artist or troupe creation request.")
public class ArtistCreateReqDTO {

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Artist or troupe name.", example = "Aurora Band")
    private String name;

    @Size(max = 512)
    @Schema(description = "Artist or troupe description.")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
