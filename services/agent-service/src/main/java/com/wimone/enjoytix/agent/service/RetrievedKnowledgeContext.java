package com.wimone.enjoytix.agent.service;

public record RetrievedKnowledgeContext(String citationKey, String title, String sourceLocation, String content) {
    public RetrievedKnowledgeContext {
        requireText(citationKey, "citation key");
        requireText(title, "title");
        requireText(sourceLocation, "source location");
        requireText(content, "content");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
