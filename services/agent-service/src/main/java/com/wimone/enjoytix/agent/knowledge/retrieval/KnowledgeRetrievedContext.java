package com.wimone.enjoytix.agent.knowledge.retrieval;

public record KnowledgeRetrievedContext(String citationKey, String title, String sourceLocation, String content) {
    public KnowledgeRetrievedContext {
        require(citationKey, "citation key"); require(title, "title"); require(sourceLocation, "source location"); require(content, "content");
    }
    private static void require(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required"); }
}