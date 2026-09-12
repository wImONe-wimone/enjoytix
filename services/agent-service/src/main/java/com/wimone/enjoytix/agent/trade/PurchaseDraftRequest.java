package com.wimone.enjoytix.agent.trade;

import java.util.List;

public record PurchaseDraftRequest(Long showId, Long categoryId, List<Long> seatIds,
                                   int quantity) {
    public PurchaseDraftRequest {
        if (showId == null || showId <= 0 || categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("showId and categoryId must be positive");
        }
        if (seatIds == null || seatIds.isEmpty() || seatIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("seatIds must contain positive ids");
        }
        if (quantity <= 0 || quantity != seatIds.size()) {
            throw new IllegalArgumentException("quantity must match seat count");
        }
        seatIds = List.copyOf(seatIds);
    }
}
