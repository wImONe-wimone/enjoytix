package com.wimone.enjoytix.agent.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.auth.AgentAuthenticationContext;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.CommentReadRemoteService;
import com.wimone.enjoytix.agent.remote.OrderReadRemoteService;
import com.wimone.enjoytix.agent.remote.PerformanceRemoteService;
import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;
import com.wimone.enjoytix.agent.remote.UserReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.CurrentUserResponse;
import com.wimone.enjoytix.agent.remote.dto.OrderDetailResponse;
import com.wimone.enjoytix.agent.remote.dto.PerformanceDetailResponse;
import com.wimone.enjoytix.agent.remote.dto.PerformancePageResponse;
import com.wimone.enjoytix.agent.remote.dto.PerformanceRatingSummaryResponse;
import com.wimone.enjoytix.agent.remote.dto.SeatAvailabilityResponse;
import com.wimone.enjoytix.agent.remote.dto.ShowSessionResponse;
import com.wimone.enjoytix.agent.remote.dto.TicketAvailabilityResponse;
import com.wimone.enjoytix.agent.tool.AgentTool;
import com.wimone.enjoytix.agent.tool.AgentToolExecutor;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import com.wimone.enjoytix.agent.tool.CurrentUserQueryTool;
import com.wimone.enjoytix.agent.tool.InMemoryAgentToolRegistry;
import com.wimone.enjoytix.agent.tool.OrderListQueryTool;
import com.wimone.enjoytix.agent.tool.PerformanceDetailQueryTool;
import com.wimone.enjoytix.agent.tool.PerformanceRatingSummaryQueryTool;
import com.wimone.enjoytix.agent.tool.SeatAvailabilityQueryTool;
import com.wimone.enjoytix.agent.tool.ShowSessionQueryTool;
import com.wimone.enjoytix.agent.tool.ShowSessionRecord;
import com.wimone.enjoytix.agent.tool.TicketAvailabilityQueryTool;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpReadOnlyGuidanceFlowTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @AfterEach
    void clearUserContext() {
        AgentUserContextHolder.clear();
    }

    @Test
    void completesAuthenticatedReadOnlyGuidanceAndOrderQueryFlow() throws Exception {
        McpController controller = controllerWithReadOnlyTools();

        Map<?, ?> listResponse = controller.handle(new MockHttpServletRequest(),
                new McpJsonRpcRequest("list", "tools/list", Map.of()));
        List<?> tools = (List<?>) ((Map<?, ?>) listResponse.get("result")).get("tools");
        List<String> toolNames = tools.stream().map(tool -> (String) ((Map<?, ?>) tool).get("name")).toList();
        assertThat(toolNames).contains(
                "search_show_sessions", "get_performance_detail", "get_performance_rating_summary",
                "get_ticket_availability", "get_seat_availability", "get_current_user", "list_current_user_orders");

        JsonNode sessions = call(controller, "search_show_sessions", Map.of(
                "city", "Shanghai", "date", "2026-10-01", "keyword", "Jazz"));
        assertThat(sessions.get(0).path("showId").asLong()).isEqualTo(21L);

        JsonNode detail = call(controller, "get_performance_detail", Map.of("performanceId", 9L));
        assertThat(detail.path("title").asText()).isEqualTo("Jazz Night");

        JsonNode ratings = call(controller, "get_performance_rating_summary", Map.of("performanceId", 9L));
        assertThat(ratings.path("reviewCount").asInt()).isEqualTo(8);
        assertThat(ratings.path("avgRating").decimalValue()).isEqualByComparingTo("4.50");

        JsonNode availability = call(controller, "get_ticket_availability", Map.of("showId", 21L));
        assertThat(availability.get(0).path("availableStock").asInt()).isEqualTo(15);

        JsonNode seats = call(controller, "get_seat_availability", Map.of("showId", 21L));
        assertThat(seats.get(0).path("status").asText()).isEqualTo("AVAILABLE");

        JsonNode user = call(controller, "get_current_user", Map.of());
        assertThat(user.path("userId").asLong()).isEqualTo(7L);

        JsonNode orders = call(controller, "list_current_user_orders", Map.of("userId", 999L));
        assertThat(orders.get(0).path("userId").asLong()).isEqualTo(7L);
        assertThat(AgentUserContextHolder.current()).isNull();
    }

    private McpController controllerWithReadOnlyTools() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        TicketReadRemoteService ticketReadRemoteService = new TicketReadRemoteService() {
            @Override
            public Result<List<TicketAvailabilityResponse>> availability(Long showId) {
                return Result.success(List.of(new TicketAvailabilityResponse(showId, 5L, "VIP",
                        BigDecimal.valueOf(299), 20, 2, 3, 15, 1)));
            }

            @Override
            public Result<List<SeatAvailabilityResponse>> seats(Long showId) {
                return Result.success(List.of(new SeatAvailabilityResponse(showId, 5L, 8L, 2L, 3, 4, "A-4", "AVAILABLE")));
            }
        };
        registry.register(new ShowSessionQueryTool((city, date, keyword) -> List.of(
                new ShowSessionRecord(21L, "Shanghai", LocalDateTime.of(2026, 10, 1, 19, 30), "Jazz Night"))));
        registry.register(new PerformanceDetailQueryTool(new PerformanceRemoteService() {
            @Override
            public Result<PageResponse<PerformancePageResponse.PerformanceSummary>> page(
                    String title, LocalDate showDate, Integer status, long current, long size) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Result<PerformanceDetailResponse> detail(Long performanceId) {
                return Result.success(new PerformanceDetailResponse(performanceId, "Jazz Night", "Shanghai",
                        List.of(new ShowSessionResponse(21L, LocalDateTime.of(2026, 10, 1, 19, 30)))));
            }
        }));
        registry.register(new PerformanceRatingSummaryQueryTool((CommentReadRemoteService) performanceId -> Result.success(
                new PerformanceRatingSummaryResponse(performanceId, 8, new BigDecimal("4.50"), 0, 0, 1, 2, 5))));
        registry.register(new TicketAvailabilityQueryTool(ticketReadRemoteService));
        registry.register(new SeatAvailabilityQueryTool(ticketReadRemoteService));
        registry.register(new CurrentUserQueryTool((UserReadRemoteService) () -> Result.success(
                new CurrentUserResponse(7L, "alice", "13800000000", "Alice", 1))));
        registry.register(new OrderListQueryTool(new OrderReadRemoteService() {
            @Override
            public Result<OrderDetailResponse> detail(Long orderId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Result<List<OrderDetailResponse>> page() {
                return Result.success(List.of(new OrderDetailResponse(31L, "ORD-31", 7L, 21L, 41L,
                        BigDecimal.valueOf(299), "PENDING_PAYMENT", null, List.of())));
            }
        }));
        AgentToolExecutor executor = (toolName, request) -> registry.find(toolName)
                .map(tool -> tool.execute(request))
                .orElseGet(() -> AgentToolResult.failure("TOOL_NOT_FOUND", "Unknown agent tool"));
        return new McpController(registry, executor, objectMapper,
                new AgentAuthenticationContext(request -> new AgentUserContext(7L, "alice")));
    }

    private JsonNode call(McpController controller, String toolName, Map<String, Object> arguments) throws Exception {
        Map<?, ?> response = controller.handle(new MockHttpServletRequest(), new McpJsonRpcRequest(toolName,
                "tools/call", Map.of("name", toolName, "arguments", arguments)));
        Map<?, ?> result = (Map<?, ?>) response.get("result");
        Map<?, ?> content = (Map<?, ?>) ((List<?>) result.get("content")).get(0);
        assertThat(result.get("isError")).isEqualTo(false);
        return objectMapper.readTree((String) content.get("text"));
    }
}
