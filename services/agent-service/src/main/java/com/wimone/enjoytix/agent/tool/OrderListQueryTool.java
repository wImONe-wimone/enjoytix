package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationException;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.OrderReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.OrderDetailResponse;
import com.wimone.enjoytix.framework.convention.result.Result;

import java.util.List;
import java.util.Map;

public class OrderListQueryTool implements AgentTool {
    private final OrderReadRemoteService orderReadRemoteService;

    public OrderListQueryTool(OrderReadRemoteService orderReadRemoteService) {
        this.orderReadRemoteService = orderReadRemoteService;
    }

    @Override
    public String name() {
        return "list_current_user_orders";
    }

    @Override
    public String description() {
        return "List orders belonging to the authenticated user.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            AgentUserContext context = AgentUserContextHolder.requireCurrent();
            Result<List<OrderDetailResponse>> result = orderReadRemoteService.page();
            if (result == null || !result.isSuccess()) {
                return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query orders");
            }
            List<OrderDetailResponse> orders = result.getData() == null ? List.of() : result.getData();
            if (orders.stream().anyMatch(order -> order == null || !context.userId().equals(order.userId()))) {
                return AgentToolResult.failure("FORBIDDEN", "Unable to access orders");
            }
            return AgentToolResult.success(orders);
        } catch (AgentAuthenticationException ex) {
            return AgentToolResult.failure("UNAUTHENTICATED", "Authenticated user context is required");
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query orders");
        }
    }
}
