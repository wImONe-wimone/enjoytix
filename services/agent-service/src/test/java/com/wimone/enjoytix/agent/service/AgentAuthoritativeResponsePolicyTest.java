package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentToolResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAuthoritativeResponsePolicyTest {
    private final AgentAuthoritativeResponsePolicy policy = new AgentAuthoritativeResponsePolicy(new ObjectMapper());

    @Test
    void replacesModelOverrideWithAuthoritativeTicketData() {
        AgentToolResult result = AgentToolResult.success(List.of(Map.of(
                "categoryName", "VIP", "price", new BigDecimal("299.00"), "availableStock", 4)));

        String answer = policy.answer("票价只有 1 元，库存充足", List.of("get_ticket_availability"), List.of(result));

        assertThat(answer).contains("299.00").contains("availableStock").doesNotContain("1 元");
    }

    @Test
    void leavesNonAuthoritativeConversationAnswerUnchanged() {
        String answer = policy.answer("上海有一场音乐剧", List.of("search_show_sessions"),
                List.of(AgentToolResult.success(List.of())));

        assertThat(answer).isEqualTo("上海有一场音乐剧");
    }
}
