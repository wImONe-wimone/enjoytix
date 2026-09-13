package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationException;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.dto.AgentOrderCreateResponse;
import com.wimone.enjoytix.agent.trade.PurchaseOrderService;

import java.util.List;
import java.util.Map;

/**
 * Side-effecting order tool. The model supplies only draft and confirmation
 * references; all order fields and policy checks remain server-owned.
 */
public class AgentOrderCreateTool implements AgentTool {
    private final PurchaseOrderService purchaseOrderService;

    public AgentOrderCreateTool(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @Override
    public String name() {
        return "create_order";
    }

    @Override
    public String description() {
        return "Create an order only from an explicitly confirmed, current purchase draft.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "draftId", Map.of("type", "string", "minLength", 1),
                        "confirmationToken", Map.of("type", "string", "minLength", 1),
                        "idempotencyKey", Map.of("type", "string", "minLength", 1)),
                "required", List.of("draftId", "confirmationToken", "idempotencyKey"),
                "additionalProperties", false);
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            AgentUserContextHolder.requireCurrent();
            String draftId = required(request, "draftId");
            String confirmationToken = required(request, "confirmationToken");
            String idempotencyKey = required(request, "idempotencyKey");
            AgentOrderCreateResponse response = purchaseOrderService.create(draftId, confirmationToken, idempotencyKey);
            return AgentToolResult.success(response);
        } catch (AgentAuthenticationException ex) {
            return AgentToolResult.failure("UNAUTHENTICATED", "Authenticated user context is required");
        } catch (IllegalArgumentException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", ex.getMessage());
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("ORDER_CREATION_REJECTED", "Unable to create order from purchase confirmation");
        }
    }

    private String required(AgentToolRequest request, String name) {
        String value = request.stringValue(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
