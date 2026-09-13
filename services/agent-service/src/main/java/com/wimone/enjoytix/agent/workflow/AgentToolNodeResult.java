package com.wimone.enjoytix.agent.workflow;

import com.wimone.enjoytix.agent.tool.AgentToolResult;

public record AgentToolNodeResult(String nodeId, String toolName, boolean success,
                                  String errorCode, String errorMessage) {
    public static AgentToolNodeResult from(String nodeId, String toolName, AgentToolResult result) {
        return new AgentToolNodeResult(nodeId, toolName, result.success(), result.errorCode(), result.errorMessage());
    }
}
