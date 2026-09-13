package com.wimone.enjoytix.agent.knowledge.indexing;

import org.springframework.ai.document.Document;
import java.util.Map;

public record KnowledgeIndexDocument(String id, String content, Map<String,Object> metadata,
                                     String knowledgeBaseId, String documentId, String versionId, String chunkId,
                                     String contentFingerprint, String versionFingerprint, String generationId,
                                     String embeddingModel) {
    public KnowledgeIndexDocument {
        if (id == null || id.isBlank() || content == null || content.isBlank()) throw new IllegalArgumentException("index document identity and content are required");
        metadata = Map.copyOf(metadata);
    }
    public Document toVectorDocument() { return Document.builder().id(id).text(content).metadata(metadata).build(); }
}
