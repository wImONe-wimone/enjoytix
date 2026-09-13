package com.wimone.enjoytix.agent.knowledge.indexing;
public record IndexingProgress(String generationId, IndexingStatus status, int totalChunks, int embeddedChunks, int attempts, int batches, boolean activated, String failureMessage) {
    static IndexingProgress failed(String id, int total, int attempts, int batches, String message) { return new IndexingProgress(id, IndexingStatus.FAILED, total, 0, attempts, batches, false, message); }
}
