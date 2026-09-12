package com.wimone.enjoytix.agent.tool;

import com.wimone.enjoytix.agent.auth.AgentAuthenticationException;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.auth.AgentUserContextHolder;
import com.wimone.enjoytix.agent.remote.UserReadRemoteService;
import com.wimone.enjoytix.agent.remote.dto.CurrentUserResponse;
import com.wimone.enjoytix.framework.convention.result.Result;

import java.util.Map;

public class CurrentUserQueryTool implements AgentTool {
    private final UserReadRemoteService userReadRemoteService;

    public CurrentUserQueryTool(UserReadRemoteService userReadRemoteService) {
        this.userReadRemoteService = userReadRemoteService;
    }

    @Override
    public String name() {
        return "get_current_user";
    }

    @Override
    public String description() {
        return "Get the authenticated user's profile.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
    }

    @Override
    public AgentToolResult execute(AgentToolRequest request) {
        try {
            AgentUserContext context = AgentUserContextHolder.requireCurrent();
            Result<CurrentUserResponse> result = userReadRemoteService.me();
            if (result == null || !result.isSuccess()) {
                return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query current user");
            }
            CurrentUserResponse user = result.getData();
            if (user == null || !context.userId().equals(user.userId())) {
                return AgentToolResult.failure("FORBIDDEN", "Unable to access current user");
            }
            return AgentToolResult.success(user);
        } catch (AgentAuthenticationException ex) {
            return AgentToolResult.failure("UNAUTHENTICATED", "Authenticated user context is required");
        } catch (RuntimeException ex) {
            return AgentToolResult.failure("TOOL_EXECUTION_FAILED", "Unable to query current user");
        }
    }
}
