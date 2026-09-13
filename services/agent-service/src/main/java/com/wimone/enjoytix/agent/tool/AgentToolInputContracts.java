package com.wimone.enjoytix.agent.tool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class AgentToolInputContracts {
    private AgentToolInputContracts() {
    }

    public record ShowIdInput(long showId) {
        public static ShowIdInput from(AgentToolRequest request) {
            return new ShowIdInput(requirePositiveLong(request, "showId"));
        }
    }

    public record PerformanceIdInput(long performanceId) {
        public static PerformanceIdInput from(AgentToolRequest request) {
            return new PerformanceIdInput(requirePositiveLong(request, "performanceId"));
        }
    }

    public record OrderIdInput(long orderId) {
        public static OrderIdInput from(AgentToolRequest request) {
            return new OrderIdInput(requirePositiveLong(request, "orderId"));
        }
    }

    public record ShowSessionInput(String city, LocalDate date, String keyword) {
        public static ShowSessionInput from(AgentToolRequest request) {
            String dateValue = request.stringValue("date");
            LocalDate date = null;
            if (dateValue != null && !dateValue.isBlank()) {
                try {
                    date = LocalDate.parse(dateValue);
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("date must use ISO-8601 format: yyyy-MM-dd", ex);
                }
            }
            return new ShowSessionInput(request.stringValue("city"), date, request.stringValue("keyword"));
        }
    }

    private static long requirePositiveLong(AgentToolRequest request, String argumentName) {
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
}
