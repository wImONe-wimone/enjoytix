package com.wimone.enjoytix.agent.tool;

import java.util.Map;

public interface AgentTool {
    String name();

    String description();

    Map<String, Object> inputSchema();

    AgentToolResult execute(AgentToolRequest request);
}