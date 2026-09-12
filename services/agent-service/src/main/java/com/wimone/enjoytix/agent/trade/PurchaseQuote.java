package com.wimone.enjoytix.agent.trade;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseQuote(List<Long> seatIds, BigDecimal unitPrice, BigDecimal totalAmount) {
    public PurchaseQuote {
        seatIds = List.copyOf(seatIds);
    }
}
