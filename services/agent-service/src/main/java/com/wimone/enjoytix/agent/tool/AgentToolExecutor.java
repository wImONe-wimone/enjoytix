package com.wimone.enjoytix.agent.tool;

public interface AgentToolExecutor {
    AgentToolResult execute(String toolName, AgentToolRequest request);

    default AgentToolResult execute(String toolName, AgentToolRequest request, AgentToolExecutionContext context) {
        return execute(toolName, request);
    }
}
