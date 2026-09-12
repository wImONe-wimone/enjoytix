package com.wimone.enjoytix.agent.trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseDraftResponse(String draftId, Long showId, Long categoryId, List<Long> seatIds,
                                    int quantity, BigDecimal unitPrice, BigDecimal totalAmount,
                                    Instant expiresAt) {
    public static PurchaseDraftResponse from(PurchaseDraft draft) {
        return new PurchaseDraftResponse(draft.draftId(), draft.showId(), draft.categoryId(), draft.seatIds(),
                draft.quantity(), draft.unitPrice(), draft.totalAmount(), draft.expiresAt());
    }
}
