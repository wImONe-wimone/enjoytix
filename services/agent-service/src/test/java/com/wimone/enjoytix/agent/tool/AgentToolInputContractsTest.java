package com.wimone.enjoytix.agent.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentToolInputContractsTest {
    @Test
    void parsesPositiveIdentifierInputs() {
        assertThat(AgentToolInputContracts.ShowIdInput.from(
                new AgentToolRequest(Map.of("showId", 12))).showId()).isEqualTo(12L);
        assertThat(AgentToolInputContracts.PerformanceIdInput.from(
                new AgentToolRequest(Map.of("performanceId", "34"))).performanceId()).isEqualTo(34L);
        assertThat(AgentToolInputContracts.OrderIdInput.from(
                new AgentToolRequest(Map.of("orderId", 56))).orderId()).isEqualTo(56L);
    }

    @Test
    void rejectsMissingMalformedAndNonPositiveIdentifiers() {
        assertThatThrownBy(() -> AgentToolInputContracts.ShowIdInput.from(new AgentToolRequest(Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("showId is required");
        assertThatThrownBy(() -> AgentToolInputContracts.PerformanceIdInput.from(
                new AgentToolRequest(Map.of("performanceId", "bad"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("performanceId must be a positive integer");
        assertThatThrownBy(() -> AgentToolInputContracts.OrderIdInput.from(
                new AgentToolRequest(Map.of("orderId", 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderId must be positive");
    }

    @Test
    void parsesOptionalShowSessionFilters() {
        AgentToolInputContracts.ShowSessionInput input = AgentToolInputContracts.ShowSessionInput.from(
                new AgentToolRequest(Map.of("city", " Shanghai ", "date", "2026-10-01", "keyword", " Jazz ")));

        assertThat(input.city()).isEqualTo("Shanghai");
        assertThat(input.date()).hasToString("2026-10-01");
        assertThat(input.keyword()).isEqualTo("Jazz");
    }
}
