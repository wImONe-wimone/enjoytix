package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationException;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.OrderReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.OrderDetailResponse;
import com.wimone.enjoytix.framework.convention.result.Result;

import java.util.List;
import java.util.Map;

public class OrderDetailQueryTool implements AgentTool {
    private final OrderReadRemoteService orderReadRemoteService;

    public OrderDetailQueryTool(OrderReadRemoteService orderReadRemoteService) {
        this.orderReadRemoteService = orderReadRemoteService;
    }

    @Override
    public String name() {
        return "get_current_user_order";
    }

    @Override
    public String description() {
        return "Get one order belonging to the authenticated user.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("orderId", Map.of("type", "integer", "minimum", 1)),
                "required", List.of("orderId"),
                "additionalProperties", false
        );
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            AgentUserContext context = AgentUserContextHolder.requireCurrent();
            Result<OrderDetailResponse> result = orderReadRemoteService.detail(
                    AgentToolInputContracts.OrderIdInput.from(request).orderId());
            if (result == null || !result.isSuccess()) {
                return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query order");
            }
            OrderDetailResponse order = result.getData();
            if (order == null || !context.userId().equals(order.userId())) {
                return AgentToolResult.failure("FORBIDDEN", "Unable to access order");
            }
            return AgentToolResult.success(order);
        } catch (AgentAuthenticationException ex) {
            return AgentToolResult.failure("UNAUTHENTICATED", "Authenticated user context is required");
        } catch (IllegalArgumentException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", ex.getMessage());
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query order");
        }
    }
}
