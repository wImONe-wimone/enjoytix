package com.wimone.enjoytix.agent.trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseDraft(String draftId, Long userId, Long showId, Long categoryId,
                            List<Long> seatIds, int quantity, BigDecimal unitPrice,
                            BigDecimal totalAmount, String parameterFingerprint, Instant expiresAt) {
}
