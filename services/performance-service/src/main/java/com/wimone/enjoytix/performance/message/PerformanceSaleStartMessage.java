package com.wimone.enjoytix.performance.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public record PerformanceSaleStartMessage(
        Long operatorId,
        Long performanceId,
        LocalDateTime scheduledSaleTime
) {

    public PerformanceSaleStartMessage {
        scheduledSaleTime = normalizeSaleTime(scheduledSaleTime);
    }

    @JsonIgnore
    public String messageKey() {
        return "performance-sale-start:" + performanceId + ":" + scheduledSaleTime;
    }

    public static LocalDateTime normalizeSaleTime(LocalDateTime saleTime) {
        if (saleTime == null) {
            return null;
        }
        return saleTime.withNano((saleTime.getNano() / 1_000_000) * 1_000_000);
    }
}
