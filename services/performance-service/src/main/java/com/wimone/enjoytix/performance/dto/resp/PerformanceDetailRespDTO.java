package com.wimone.enjoytix.performance.dto.resp;

import java.util.List;

public record PerformanceDetailRespDTO(
        Long performanceId,
        String title,
        String performanceType,
        String city,
        String posterUrl,
        String description,
        Integer status,
        ArtistRespDTO artist,
        VenueRespDTO venue,
        List<ShowSessionRespDTO> sessions) {
}
