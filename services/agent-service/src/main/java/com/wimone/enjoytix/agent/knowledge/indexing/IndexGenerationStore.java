package com.wimone.enjoytix.agent.knowledge.indexing;
public interface IndexGenerationStore {
    void create(String generationId, String knowledgeBaseId, String documentId, String versionId, String fingerprint, int totalChunks);
    void markReady(String generationId, int embeddedChunks);
    void activate(String generationId);
    void markFailed(String generationId, String message);
}
