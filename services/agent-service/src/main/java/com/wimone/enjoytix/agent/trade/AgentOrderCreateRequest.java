package com.wimone.enjoytix.agent.trade;
import java.util.List;

public record AgentOrderCreateRequest(String draftId, String confirmationToken, Long showId,
                                      Long categoryId, int quantity, List<Long> seatIds) {
    public AgentOrderCreateRequest {
        if (draftId == null || draftId.isBlank() || confirmationToken == null || confirmationToken.isBlank()) {
            throw new IllegalArgumentException("draftId and confirmationToken are required");
        }
        if (showId == null || categoryId == null || quantity <= 0 || seatIds == null || seatIds.size() != quantity) {
            throw new IllegalArgumentException("order draft fields are invalid");
        }
        seatIds = List.copyOf(seatIds);
    }

    public static AgentOrderCreateRequest from(String draftId, String confirmationToken, PurchaseDraft draft) {
        return new AgentOrderCreateRequest(draftId, confirmationToken, draft.showId(), draft.categoryId(),
                draft.quantity(), draft.seatIds());
    }
}
