package com.wimone.enjoytix.agent.tool;

import java.time.LocalDate;
import java.util.List;

public class InMemoryShowSessionQueryService implements ShowSessionQueryService {
    private final List<ShowSessionRecord> sessions;

    public InMemoryShowSessionQueryService(List<ShowSessionRecord> sessions) {
        this.sessions = sessions == null ? List.of() : List.copyOf(sessions);
    }

    @Override
    public List<ShowSessionRecord> query(String city, LocalDate date, String keyword) {
        return sessions.stream()
                .filter(session -> city == null || city.isBlank() || city.equalsIgnoreCase(session.city()))
                .filter(session -> date == null || date.equals(session.showTime().toLocalDate()))
                .filter(session -> keyword == null || keyword.isBlank()
                        || session.title().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}