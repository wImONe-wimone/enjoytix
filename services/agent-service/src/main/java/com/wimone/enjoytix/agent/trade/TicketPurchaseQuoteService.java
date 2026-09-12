package com.wimone.enjoytix.agent.trade;

public interface TicketPurchaseQuoteService {
    PurchaseQuote quote(PurchaseDraftRequest request);
}
