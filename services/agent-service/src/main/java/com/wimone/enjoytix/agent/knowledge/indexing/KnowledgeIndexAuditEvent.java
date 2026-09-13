package com.wimone.enjoytix.agent.knowledge.indexing;
public record KnowledgeIndexAuditEvent(String subject, String knowledgeBaseId, String documentId, String versionId, String contentFingerprint, String outcome, String failureCategory, int chunkCount, int attempts) {
    public KnowledgeIndexAuditEvent {
        if (subject == null || subject.isBlank() || knowledgeBaseId == null || knowledgeBaseId.isBlank() || documentId == null || documentId.isBlank() || outcome == null || outcome.isBlank()) throw new IllegalArgumentException("audit identity and outcome are required");
        if (versionId != null && versionId.contains("object")) throw new IllegalArgumentException("audit must not contain storage paths");
    }
    @Override public String toString() { return "KnowledgeIndexAuditEvent[subject=" + subject + ", knowledgeBaseId=" + knowledgeBaseId + ", documentId=" + documentId + ", versionId=" + versionId + ", contentFingerprint=" + contentFingerprint + ", outcome=" + outcome + ", failureCategory=" + failureCategory + ", chunkCount=" + chunkCount + ", attempts=" + attempts + "]"; }
}
