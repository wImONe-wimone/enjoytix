package com.wimone.enjoytix.agent.trade;
import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
public class PurchaseOrderService {
    private final PurchaseDraftService draftService;
    private final AgentOrderCreateRemoteService orderRemoteService;
    public PurchaseOrderService(PurchaseDraftService draftService, AgentOrderCreateRemoteService orderRemoteService) { this.draftService = draftService; this.orderRemoteService = orderRemoteService; }
    public AgentOrderCreateResponse create(String draftId, String confirmationToken) {
        PurchaseDraft draft;
        try {
            draft = draftService.consume(confirmationToken, draftId);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("purchase confirmation is invalid");
        }
        Result<AgentOrderCreateResponse> result = orderRemoteService.create(AgentOrderCreateRequest.from(draftId, confirmationToken, draft));
        if (result == null || !result.isSuccess() || result.getData() == null) throw new IllegalStateException("order creation failed");
        return result.getData();
    }
}
