package com.wimone.enjoytix.agent.trade;

import jakarta.validation.constraints.NotBlank;

public record PurchaseConfirmationRequest(@NotBlank String draftId) {
}
