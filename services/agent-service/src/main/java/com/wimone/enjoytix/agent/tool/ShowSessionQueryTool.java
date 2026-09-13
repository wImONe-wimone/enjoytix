package com.wimone.enjoytix.agent.tool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ShowSessionQueryTool implements AgentTool {
    private final ShowSessionQueryService queryService;

    public ShowSessionQueryTool(ShowSessionQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public String name() {
        return "search_show_sessions";
    }

    @Override
    public String description() {
        return "Search performance show sessions by city, date, or keyword.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string"),
                        "date", Map.of("type", "string", "format", "date"),
                        "keyword", Map.of("type", "string")
                ),
                "additionalProperties", false
        );
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            AgentToolInputContracts.ShowSessionInput input = AgentToolInputContracts.ShowSessionInput.from(request);
            return AgentToolResult.success(queryService.query(input.city(), input.date(), input.keyword()));
        } catch (IllegalArgumentException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", ex.getMessage());
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query show sessions");
        }
    }
}
