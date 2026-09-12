package com.wimone.enjoytix.agent.tool;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentToolTest {

    @Test
    void registryListsAndResolvesToolsByName() {
        InMemoryAgentToolRegistry registry = new InMemoryAgentToolRegistry();
        AgentTool tool = new StubTool("search_shows");

        registry.register(tool);

        assertThat(registry.find("search_shows")).containsSame(tool);
        assertThat(registry.list()).containsExactly(tool);
        assertThatThrownBy(() -> registry.register(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void showSessionToolFiltersByCityDateAndKeyword() {
        InMemoryShowSessionQueryService queryService = new InMemoryShowSessionQueryService(List.of(
                new ShowSessionRecord(1L, "Shanghai", LocalDateTime.of(2026, 10, 1, 19, 30), "Jazz Night"),
                new ShowSessionRecord(2L, "Beijing", LocalDateTime.of(2026, 10, 1, 19, 30), "Rock Live"),
                new ShowSessionRecord(3L, "Shanghai", LocalDateTime.of(2026, 10, 2, 19, 30), "Jazz Night")
        ));
        ShowSessionQueryTool tool = new ShowSessionQueryTool(queryService);

        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of(
                "city", "Shanghai",
                "date", "2026-10-01",
                "keyword", "Jazz"
        )));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(List.of(
                new ShowSessionRecord(1L, "Shanghai", LocalDateTime.of(2026, 10, 1, 19, 30), "Jazz Night")
        ));
    }

    @Test
    void showSessionToolRejectsInvalidDate() {
        ShowSessionQueryTool tool = new ShowSessionQueryTool(new InMemoryShowSessionQueryService(List.of()));

        AgentToolResult result = tool.execute(new AgentToolRequest(Map.of("date", "tomorrow")));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_ARGUMENT");
    }

    private record StubTool(String name) implements AgentTool {
        @Override
        public String description() {
            return "stub";
        }

        @Override
        public Map<String, Object> inputSchema() {
            return Map.of();
        }

        @Override
        public AgentToolResult execute(AgentToolRequest request) {
            return AgentToolResult.success(List.of());
        }
    }
}