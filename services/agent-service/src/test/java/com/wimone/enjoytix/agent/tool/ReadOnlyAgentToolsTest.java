package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.OrderReadRemoteService;
import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.CurrentUserResponse;
import com.wimone.enjoytix.agent.remote.dto.OrderDetailResponse;
import com.wimone.enjoytix.agent.remote.dto.OrderItemResponse;
import com.wimone.enjoytix.agent.remote.dto.SeatAvailabilityResponse;
import com.wimone.enjoytix.agent.remote.dto.TicketAvailabilityResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReadOnlyAgentToolsTest {

    @AfterEach
    void clearUserContext() {
        AgentUserContextHolder.clear();
    }

    @Test
    void ticketAvailabilityToolRequiresPositiveShowIdAndReturnsRemoteData() {
        TicketAvailabilityQueryTool tool = new TicketAvailabilityQueryTool(new TicketReadRemoteService() {
            @Override
            public Result<List<TicketAvailabilityResponse>> availability(Long showId) {
                return Result.success(List.of(new TicketAvailabilityResponse(showId, 5L, "VIP",
                        BigDecimal.valueOf(299), 20, 2, 3, 15, 1)));
            }

            @Override
            public Result<List<SeatAvailabilityResponse>> seats(Long showId) {
                throw new UnsupportedOperationException();
            }
        });

        AgentToolResult invalid = tool.execute(new AgentToolRequest(Map.of("showId", "0")));
        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of("showId", 12L)));

        assertThat(invalid.success()).isFalse();
        assertThat(invalid.errorCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(List.of(new TicketAvailabilityResponse(12L, 5L, "VIP",
                BigDecimal.valueOf(299), 20, 2, 3, 15, 1)));
    }

    @Test
    void seatAvailabilityToolReturnsRemoteData() {
        SeatAvailabilityQueryTool tool = new SeatAvailabilityQueryTool(new TicketReadRemoteService() {
            @Override
            public Result<List<TicketAvailabilityResponse>> availability(Long showId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Result<List<SeatAvailabilityResponse>> seats(Long showId) {
                return Result.success(List.of(new SeatAvailabilityResponse(showId, 5L, 8L, 2L, 3, 4, "A-4", "AVAILABLE")));
            }
        });

        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of("showId", "12")));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(List.of(new SeatAvailabilityResponse(12L, 5L, 8L, 2L, 3, 4, "A-4", "AVAILABLE")));
    }

    @Test
    void currentUserToolRequiresTrustedUserContext() {
        CurrentUserQueryTool tool = new CurrentUserQueryTool(() -> Result.success(
                new CurrentUserResponse(7L, "alice", "13800000000", "Alice", 1)));

        AgentToolResult unauthenticated = tool.execute(new AgentToolRequest(Map.of()));
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of()));

        assertThat(unauthenticated.success()).isFalse();
        assertThat(unauthenticated.errorCode()).isEqualTo("UNAUTHENTICATED");
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(new CurrentUserResponse(7L, "alice", "13800000000", "Alice", 1));
    }

    @Test
    void orderListToolRequiresTrustedUserContextAndHasNoUserIdArgument() {
        OrderListQueryTool tool = new OrderListQueryTool(new OrderReadRemoteService() {
            @Override
            public Result<OrderDetailResponse> detail(Long orderId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Result<List<OrderDetailResponse>> page() {
                return Result.success(List.of(order(7L, 11L)));
            }
        });

        AgentToolResult unauthenticated = tool.execute(new AgentToolRequest(Map.of()));
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));
        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of()));

        assertThat(tool.inputSchema().toString()).doesNotContain("userId");
        assertThat(unauthenticated.errorCode()).isEqualTo("UNAUTHENTICATED");
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(List.of(order(7L, 11L)));
    }

    @Test
    void orderDetailToolRejectsReturnedOrderOwnedByAnotherUser() {
        OrderDetailQueryTool tool = new OrderDetailQueryTool(new OrderReadRemoteService() {
            @Override
            public Result<OrderDetailResponse> detail(Long orderId) {
                return Result.success(order(8L, orderId));
            }

            @Override
            public Result<List<OrderDetailResponse>> page() {
                throw new UnsupportedOperationException();
            }
        });
        AgentUserContextHolder.set(new AgentUserContext(7L, "alice"));

        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of("orderId", 11L)));

        assertThat(tool.inputSchema().toString()).doesNotContain("userId");
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("FORBIDDEN");
        assertThat(result.data()).isNull();
    }

    private static OrderDetailResponse order(Long userId, Long orderId) {
        return new OrderDetailResponse(orderId, "ORD-" + orderId, userId, 12L, 13L,
                BigDecimal.valueOf(299), "PENDING_PAYMENT", null,
                List.of(new OrderItemResponse(1L, 12L, 5L, 1, List.of(8L),
                        BigDecimal.valueOf(299), BigDecimal.valueOf(299), List.of())));
    }
}
