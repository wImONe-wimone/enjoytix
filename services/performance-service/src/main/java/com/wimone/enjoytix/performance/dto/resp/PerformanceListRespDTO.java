package com.wimone.enjoytix.performance.dto.resp;

import java.time.LocalDateTime;

public record PerformanceListRespDTO(
        Long performanceId,
        String title,
        String performanceType,
        String city,
        ArtistRespDTO artist,
        VenueRespDTO venue,
        LocalDateTime earliestShowTime,
        String posterUrl,
        Integer status) {
}
