package com.wimone.enjoytix.agent.knowledge.parser;

import java.util.Map;

public record ParsedDocument(String text, Map<String, String> metadata) {
    public ParsedDocument {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("parsed text is required");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
