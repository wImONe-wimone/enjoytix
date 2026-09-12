package com.wimone.enjoytix.agent.tool;

import java.time.LocalDate;
import java.util.List;

public interface ShowSessionQueryService {
    List<ShowSessionRecord> query(String city, LocalDate date, String keyword);
}