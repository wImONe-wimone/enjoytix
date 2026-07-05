package com.wimone.enjoytix.ticket.common;

import com.wimone.enjoytix.framework.cache.CacheKeyBuilder;

public final class TicketLockKeys {

    private static final String SHOW_STOCK_LOCK = "enjoytix:ticket:show-stock-lock";

    private TicketLockKeys() {
    }

    public static String showStock(Long showId) {
        return CacheKeyBuilder.build(SHOW_STOCK_LOCK, showId);
    }
}
