package com.wimone.enjoytix.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimone.enjoytix.agent.tool.AgentToolResult;

import java.util.List;
import java.util.Set;

/**
 * Prevents model prose from becoming the source of truth for price and inventory.
 */
final class AgentAuthoritativeResponsePolicy {
    private static final Set<String> AUTHORITATIVE_TOOLS = Set.of(
            "get_ticket_availability", "get_seat_availability");

    private final ObjectMapper objectMapper;

    AgentAuthoritativeResponsePolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String answer(String modelAnswer, List<String> toolCalls, List<AgentToolResult> toolResults) {
        for (int i = 0; i < toolCalls.size() && i < toolResults.size(); i++) {
            if (AUTHORITATIVE_TOOLS.contains(toolCalls.get(i))) {
                AgentToolResult result = toolResults.get(i);
                if (!result.success()) {
                    return result.errorMessage() == null ? "Unable to query authoritative ticket data" : result.errorMessage();
                }
                return serialize(result.data());
            }
        }
        return modelAnswer == null ? "" : modelAnswer;
    }

    private String serialize(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            return "Unable to format authoritative ticket data";
        }
    }
}
