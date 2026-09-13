package com.wimone.enjoytix.agent.knowledge.indexing;
public record KnowledgeIndexTombstone(String knowledgeBaseId, String documentId, String versionId, String chunkId, String fingerprint, TombstoneReason reason, String vectorId) {
    public KnowledgeIndexTombstone { if (knowledgeBaseId == null || knowledgeBaseId.isBlank() || vectorId == null || vectorId.isBlank() || reason == null) throw new IllegalArgumentException("tombstone scope, vector id and reason are required"); }
    public String key() { return String.join("|", knowledgeBaseId, String.valueOf(documentId), String.valueOf(versionId), String.valueOf(chunkId), String.valueOf(fingerprint), reason.name()); }
}
