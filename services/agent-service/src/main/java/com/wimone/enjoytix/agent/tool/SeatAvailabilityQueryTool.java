package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.TicketReadRemoteService;

import java.util.List;
import java.util.Map;

public class SeatAvailabilityQueryTool implements AgentTool {
    private final TicketReadRemoteService ticketReadRemoteService;

    public SeatAvailabilityQueryTool(TicketReadRemoteService ticketReadRemoteService) {
        this.ticketReadRemoteService = ticketReadRemoteService;
    }

    @Override
    public String name() {
        return "get_seat_availability";
    }

    @Override
    public String description() {
        return "Get seat-level availability for a show session.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("showId", Map.of("type", "integer", "minimum", 1)),
                "required", List.of("showId"),
                "additionalProperties", false
        );
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            return ReadOnlyToolSupport.fromRemoteResult(
                    ticketReadRemoteService.seats(AgentToolInputContracts.ShowIdInput.from(request).showId()),
                    "Unable to query seat availability");
        } catch (IllegalArgumentException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", ex.getMessage());
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query seat availability");
        }
    }
}
