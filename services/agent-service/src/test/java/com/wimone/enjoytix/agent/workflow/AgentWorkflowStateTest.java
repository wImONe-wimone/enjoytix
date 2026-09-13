package com.wimone.enjoytix.agent.workflow;

import com.wimone.enjoytix.agent.service.AgentModelMessage;
import com.wimone.enjoytix.agent.service.AgentToolCall;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWorkflowStateTest {

    @Test
    void retainsApplicationOwnedCorrelationMessagesAndSafeToolNodeResults() {
        AgentWorkflowState initial = AgentWorkflowState.initial("conversation-1", "run-1", "Question");
        AgentWorkflowState updated = initial
                .appendAssistant("", List.of(new AgentToolCall("call-1", "lookup_order", Map.of("orderId", "secret"))))
                .appendToolResult(new AgentToolCall("call-1", "lookup_order", Map.of("orderId", "secret")),
                        AgentToolResult.failure("ORDER_NOT_FOUND", "Order is unavailable"), "{}");

        assertThat(initial.messages()).containsExactly(AgentModelMessage.user("Question"));
        assertThat(updated.conversationId()).isEqualTo("conversation-1");
        assertThat(updated.runId()).isEqualTo("run-1");
        assertThat(updated.toolNames()).containsExactly("lookup_order");
        assertThat(updated.toolResults()).containsExactly(
                new AgentToolNodeResult("call-1", "lookup_order", false, "ORDER_NOT_FOUND", "Order is unavailable"));
        assertThat(updated.toolResults().toString()).doesNotContain("orderId", "secret");
    }
}
