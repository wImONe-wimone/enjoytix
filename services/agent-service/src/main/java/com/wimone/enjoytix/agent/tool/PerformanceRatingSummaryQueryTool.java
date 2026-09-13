package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.remote.CommentReadRemoteService;

import java.util.List;
import java.util.Map;

public class PerformanceRatingSummaryQueryTool implements AgentTool {
    private final CommentReadRemoteService commentReadRemoteService;

    public PerformanceRatingSummaryQueryTool(CommentReadRemoteService commentReadRemoteService) {
        this.commentReadRemoteService = commentReadRemoteService;
    }

    @Override
    public String name() {
        return "get_performance_rating_summary";
    }

    @Override
    public String description() {
        return "Get aggregated review rating data for a performance.";
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
                    commentReadRemoteService.ratingSummary(AgentToolInputContracts.PerformanceIdInput.from(request).performanceId()),
                    "Unable to query performance rating summary");
        } catch (IllegalArgumentException ex) {
            return AgentToolResult.failure("INVALID_ARGUMENT", ex.getMessage());
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query performance rating summary");
        }
    }
}
