package com.wimone.enjoytix.agent.tool;

public interface AgentToolExecutor {
    AgentToolResult execute(String toolName, AgentToolRequest request);
}
