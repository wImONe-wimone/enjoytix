package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.framework.convention.result.Result;

final class ReadOnlyToolSupport {
    private ReadOnlyToolSupport() {
    }

    static Long requirePositiveLong(AgentToolRequest request, String argumentName) {
        String value = request.stringValue(argumentName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(argumentName + " is required");
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(argumentName + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(argumentName + " must be a positive integer", ex);
        }
    }

    static <T> AgentToolResult fromRemoteResult(Result<T> result, String failureMessage) {
        if (result == null || !result.isSuccess()) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", failureMessage);
        }
        return AgentToolResult.success(result.getData());
    }
}
