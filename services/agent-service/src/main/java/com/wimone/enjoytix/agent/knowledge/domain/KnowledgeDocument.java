package com.wimone.enjoytix.agent.knowledge.domain;

public final class KnowledgeDocument {
    private final String id;
    private final String knowledgeBaseId;
    private final String name;
    private boolean enabled;
    private KnowledgeDocumentVersion effectiveVersion;

    public KnowledgeDocument(String id, String knowledgeBaseId, String name, boolean enabled) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("document id is required");
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) throw new IllegalArgumentException("knowledge base id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("document name is required");
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.name = name;
        this.enabled = enabled;
    }

    public void activate(KnowledgeDocumentVersion version) {
        if (version == null || !id.equals(version.documentId())) throw new IllegalArgumentException("version does not belong to document");
        if (version.status() != DocumentStatus.SUCCESS || !version.effective()) {
            throw new IllegalStateException("only published successful version can be effective");
        }
        effectiveVersion = version;
    }

    public String id() { return id; }
    public String knowledgeBaseId() { return knowledgeBaseId; }
    public String name() { return name; }
    public boolean enabled() { return enabled; }
    public KnowledgeDocumentVersion effectiveVersion() { return effectiveVersion; }
}
