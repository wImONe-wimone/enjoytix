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
            String dateValue = request.stringValue("date");
            LocalDate date = dateValue == null || dateValue.isBlank() ? null : LocalDate.parse(dateValue);
            return AgentToolResult.success(queryService.query(
                    request.stringValue("city"), date, request.stringValue("keyword")));
        } catch (DateTimeParseException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", "date must use ISO-8601 format: yyyy-MM-dd");
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query show sessions");
        }
    }
}