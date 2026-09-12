package com.wimone.enjoytix.agent.trade;

import java.time.Instant;

public record PurchaseConfirmation(String token, String draftId, Instant expiresAt) {
}
