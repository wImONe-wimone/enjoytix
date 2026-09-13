package com.wimone.enjoytix.agent.workflow;

import com.wimone.enjoytix.agent.service.AgentModelMessage;
import com.wimone.enjoytix.agent.service.AgentToolCall;
import com.wimone.enjoytix.agent.tool.AgentToolResult;

import java.util.ArrayList;
import java.util.List;

public record AgentWorkflowState(String conversationId, String runId, List<AgentModelMessage> messages,
                                 List<AgentToolNodeResult> toolResults) {
    public AgentWorkflowState {
        messages = messages == null ? List.of() : List.copyOf(messages);
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
    }

    public static AgentWorkflowState initial(String conversationId, String runId, String content) {
        return new AgentWorkflowState(conversationId, runId, List.of(AgentModelMessage.user(content)), List.of());
    }

    public AgentWorkflowState appendAssistant(String content, List<AgentToolCall> toolCalls) {
        List<AgentModelMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(AgentModelMessage.assistant(content, toolCalls));
        return new AgentWorkflowState(conversationId, runId, updatedMessages, toolResults);
    }

    public AgentWorkflowState appendToolResult(AgentToolCall call, AgentToolResult result, String serializedResult) {
        List<AgentModelMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(AgentModelMessage.tool(call.name(), call.id(), serializedResult));
        List<AgentToolNodeResult> updatedToolResults = new ArrayList<>(toolResults);
        updatedToolResults.add(AgentToolNodeResult.from(call.id(), call.name(), result));
        return new AgentWorkflowState(conversationId, runId, updatedMessages, updatedToolResults);
    }

    public List<String> toolNames() {
        return toolResults.stream().map(AgentToolNodeResult::toolName).toList();
    }
}
