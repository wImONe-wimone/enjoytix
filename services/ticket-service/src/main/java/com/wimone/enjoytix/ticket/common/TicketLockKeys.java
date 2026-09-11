package com.wimone.enjoytix.ticket.common;

import com.wimone.enjoytix.framework.cache.CacheKeyBuilder;

public final class TicketLockKeys {

    private static final String SHOW_STOCK_LOCK = "enjoytix:ticket:show-stock-lock";

    private TicketLockKeys() {
    }

    public static String showStock(Long showId) {
        return CacheKeyBuilder.build(SHOW_STOCK_LOCK, showId);
    }

    public static String showCategory(Long showId, Long categoryId) {
        return CacheKeyBuilder.build(SHOW_STOCK_LOCK, showId, categoryId);
    }

    public static String showCategoryArea(Long showId, Long categoryId, Long areaId) {
        if (areaId == null) {
            return showCategory(showId, categoryId);
        }
        return CacheKeyBuilder.build(SHOW_STOCK_LOCK, showId, categoryId, areaId);
    }
}
