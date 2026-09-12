package com.wimone.enjoytix.agent.trade;

public interface PurchaseDraftService {
    PurchaseDraft createDraft(PurchaseDraftRequest request);
    PurchaseConfirmation issueConfirmation(String draftId);
    boolean confirm(String token, String draftId, PurchaseDraftRequest request);
    PurchaseDraft consume(String token, String draftId);
}
