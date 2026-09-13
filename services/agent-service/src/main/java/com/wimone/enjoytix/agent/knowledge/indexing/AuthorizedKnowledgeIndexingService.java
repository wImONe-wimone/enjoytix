package com.wimone.enjoytix.agent.knowledge.indexing;
import com.wimone.enjoytix.agent.auth.AgentUserContext;
import com.wimone.enjoytix.agent.knowledge.domain.*;
import java.util.*;
public final class AuthorizedKnowledgeIndexingService {
    private final KnowledgeIndexAuthorization authorization; private final KnowledgeIndexingJob job; private final KnowledgeIndexAuditSink audit;
    public AuthorizedKnowledgeIndexingService(KnowledgeIndexAuthorization authorization, KnowledgeIndexingJob job, KnowledgeIndexAuditSink audit) { this.authorization=Objects.requireNonNull(authorization); this.job=Objects.requireNonNull(job); this.audit=Objects.requireNonNull(audit); }
    public IndexingProgress rebuild(AgentUserContext user, KnowledgeDocument document, KnowledgeDocumentVersion version, List<KnowledgeChunk> chunks, String embeddingModel) { return execute("REBUILD", user, document, version, chunks, embeddingModel); }
    public IndexingProgress upsert(AgentUserContext user, KnowledgeDocument document, KnowledgeDocumentVersion version, List<KnowledgeChunk> chunks, String embeddingModel) { return execute("UPSERT", user, document, version, chunks, embeddingModel); }
    private IndexingProgress execute(String operation, AgentUserContext user, KnowledgeDocument document, KnowledgeDocumentVersion version, List<KnowledgeChunk> chunks, String embeddingModel) {
        String subject = "user:" + user.userId();
        if (!authorization.canIndex(user, document.knowledgeBaseId(), document.id())) { audit.record(new KnowledgeIndexAuditEvent(subject, document.knowledgeBaseId(), document.id(), version.documentId()+":v"+version.version(), contentFingerprint(chunks), "DENIED", "UNAUTHORIZED", chunks == null ? 0 : chunks.size(), 0)); return IndexingProgress.failed("unauthorized", chunks == null ? 0 : chunks.size(), 0, 0, "Knowledge indexing is not authorized."); }
        try { IndexingProgress result = "REBUILD".equals(operation) ? job.rebuild(document, version, chunks, embeddingModel) : job.upsert(document, version, chunks, embeddingModel); audit.record(new KnowledgeIndexAuditEvent(subject, document.knowledgeBaseId(), document.id(), version.documentId()+":v"+version.version(), contentFingerprint(chunks), result.status() == IndexingStatus.ACTIVE ? "SUCCESS" : "FAILED", result.failureMessage(), result.totalChunks(), result.attempts())); return result; }
        catch (RuntimeException failure) { audit.record(new KnowledgeIndexAuditEvent(subject, document.knowledgeBaseId(), document.id(), version.documentId()+":v"+version.version(), contentFingerprint(chunks), "FAILED", failure.getClass().getSimpleName(), chunks == null ? 0 : chunks.size(), 0)); return IndexingProgress.failed("failed", chunks == null ? 0 : chunks.size(), 0, 0, "Knowledge indexing failed."); }
    }
    private String contentFingerprint(List<KnowledgeChunk> chunks) { return chunks == null || chunks.isEmpty() ? null : chunks.get(0).contentHash(); }
}
