package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.PerformanceRemoteService;

import java.util.List;
import java.util.Map;

public class PerformanceDetailQueryTool implements AgentTool {
    private final PerformanceRemoteService performanceRemoteService;

    public PerformanceDetailQueryTool(PerformanceRemoteService performanceRemoteService) {
        this.performanceRemoteService = performanceRemoteService;
    }

    @Override
    public String name() {
        return "get_performance_detail";
    }

    @Override
    public String description() {
        return "Get public performance details and its show sessions.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("performanceId", Map.of("type", "integer", "minimum", 1)),
                "required", List.of("performanceId"),
                "additionalProperties", false
        );
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            return ReadOnlyToolSupport.fromRemoteResult(
                    performanceRemoteService.detail(AgentToolInputContracts.PerformanceIdInput.from(request).performanceId()),
                    "Unable to query performance detail");
        } catch (IllegalArgumentException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", ex.getMessage());
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query performance detail");
        }
    }
}
