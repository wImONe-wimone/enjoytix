package com.wimone.enjoytix.agent.trade;
import com.wimone.enjoytix.agent.remote.AgentOrderCreateRemoteService;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class PurchaseOrderService {
    private final PurchaseDraftService draftService;
    private final AgentOrderCreateRemoteService orderRemoteService;
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private final Map<String, Object> idempotencyLocks = new ConcurrentHashMap<>();
    public PurchaseOrderService(PurchaseDraftService draftService, AgentOrderCreateRemoteService orderRemoteService) { this.draftService = draftService; this.orderRemoteService = orderRemoteService; }
    public AgentOrderCreateResponse create(String draftId, String confirmationToken) {
        return create(draftId, confirmationToken, null);
    }

    public AgentOrderCreateResponse create(String draftId, String confirmationToken, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createWithoutIdempotency(draftId, confirmationToken);
        }
        Long userId = AgentUserContextHolder.requireCurrent().userId();
        String scope = userId + ":" + idempotencyKey.trim();
        synchronized (idempotencyLocks.computeIfAbsent(scope, ignored -> new Object())) {
            IdempotencyRecord previous = idempotency.get(scope);
            String requestFingerprint = draftId + "|" + confirmationToken;
            if (previous != null) {
                if (!previous.requestFingerprint().equals(requestFingerprint)) {
                    throw new IllegalArgumentException("idempotency key is already used for another request");
                }
                return previous.response();
            }
            AgentOrderCreateResponse response = createWithoutIdempotency(draftId, confirmationToken);
            idempotency.put(scope, new IdempotencyRecord(requestFingerprint, response));
            return response;
        }
    }

    private AgentOrderCreateResponse createWithoutIdempotency(String draftId, String confirmationToken) {
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

    private record IdempotencyRecord(String requestFingerprint, AgentOrderCreateResponse response) {
    }
}
